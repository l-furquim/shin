package com.shin.upload.service.impl;

import com.shin.upload.dto.PresignedChunk;
import com.shin.upload.dto.PresignedUpload;
import com.shin.upload.exceptions.PresignException;
import com.shin.upload.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class S3StorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(15);

    private final S3Client client;
    private final S3Presigner presigner;

    @Value("${spring.cloud.aws.s3.buckets.raw}")
    private String rawBucket;

    @Value("${spring.cloud.aws.s3.buckets.processed}")
    private String processedBucket;

    public S3StorageServiceImpl(S3Client client, S3Presigner presigner) {
        this.client = client;
        this.presigner = presigner;
    }

    @Override
    public void upload(String bucket, String key, byte[] data, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveBucket(bucket))
                .key(key)
                .contentType(contentType)
                .build();

            client.putObject(putObjectRequest, RequestBody.fromBytes(data));

        } catch (Exception e) {
            logger.error("Error uploading to S3: {}", e.getMessage(), e);
        }
    }

    @Override
    public void assembleChunks(List<String> sourceKeys, String destBucket, String destKey) {
        assembleChunks(sourceKeys, destBucket, destKey, Map.of());
    }

    @Override
    public void assembleChunks(List<String> sourceKeys, String destBucket, String destKey, Map<String, String> metadata) {
        List<File> chunks = new ArrayList<>();
        File outputFile = null;

        try {
            outputFile = File.createTempFile("assembled-video", ".mp4");
            logger.info("Starting to download {} chunks from S3", sourceKeys.size());

            for (int index = 0; index < sourceKeys.size(); index++) {
                String key = sourceKeys.get(index);
                try {
                    ResponseInputStream<GetObjectResponse> response = client.getObject(builder ->
                        builder.bucket(rawBucket).key(key)
                    );

                    File chunkFile = File.createTempFile("chunk-" + index, ".bin");
                    Files.copy(response, chunkFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    chunks.add(chunkFile);
                    logger.info("Downloaded chunk {}: {} (size: {} bytes)", index, key, chunkFile.length());
                } catch (Exception e) {
                    logger.error("Error downloading chunk {}: {}", key, e.getMessage(), e);
                    throw e;
                }
            }

            logger.info("Starting binary concatenation of {} chunks", chunks.size());

            try (var output = Files.newOutputStream(outputFile.toPath())) {
                for (File chunkFile : chunks) {
                    try (var input = Files.newInputStream(chunkFile.toPath())) {
                        input.transferTo(output);
                    }
                }
            }

            logger.info("Binary concatenation completed. Final file size: {} bytes", outputFile.length());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(resolveBucket(destBucket))
                .key(destKey)
                .metadata(metadata)
                .contentType("video/mp4")
                .build();

            client.putObject(putRequest, RequestBody.fromFile(outputFile));
            logger.info("Successfully uploaded assembled video to S3: {}", destKey);

        } catch (Exception e) {
            logger.error("Error assembling chunks: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            for (File chunk : chunks) {
                if (!chunk.delete()) {
                    logger.warn("Failed to delete temp chunk file: {}", chunk.getAbsolutePath());
                }
            }
            if (outputFile != null && !outputFile.delete()) {
                logger.warn("Failed to delete temp output file: {}", outputFile.getAbsolutePath());
            }
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                .bucket(resolveBucket(bucket))
                .key(key)
                .build());
        } catch (Exception e) {
            logger.error("Error deleting s3://{}/{}: {}", bucket, key, e.getMessage(), e);
        }
    }

    @Override
    public void deleteMultiple(String bucket, List<String> keys) {
        if (keys.isEmpty()) return;

        try {
            List<ObjectIdentifier> identifiers = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

            client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(resolveBucket(bucket))
                .delete(Delete.builder().objects(identifiers).build())
                .build());
        } catch (Exception e) {
            logger.error("Error deleting multiple objects from S3: {}", e.getMessage(), e);
        }
    }

    @Override
    public PresignedUpload generatePresignedUpload(String bucket, String key, String contentType,
            String videoId, String userId, String originalName, Long fileSize, List<String> resolutions) {
        try {
            Map<String, String> metadata = new HashMap<>(Map.of(
                "videoid", videoId,
                "userid", userId,
                "filename", originalName,
                "resolutions", String.join(",", resolutions)
            ));

            if (fileSize != null) {
                metadata.put("filesize", String.valueOf(fileSize));
            }
            if (contentType != null && !contentType.isBlank()) {
                metadata.put("contenttype", contentType);
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveBucket(bucket))
                .key(key)
                .metadata(metadata)
                .contentType(contentType)
                .build();

            var response = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(PRESIGN_DURATION)
                .build());

            return new PresignedUpload(response.url().toString(), response.expiration().getEpochSecond());

        } catch (Exception e) {
            logger.error("Error generating presigned upload: {}", e.getMessage(), e);
            throw new PresignException(e.getMessage());
        }
    }

    @Override
    public List<PresignedChunk> generatePresignedChunks(String bucket, List<String> keys) {
        try {
            String resolvedBucket = resolveBucket(bucket);
            List<PresignedChunk> presignedChunks = new ArrayList<>(keys.size());

            for (int i = 0; i < keys.size(); i++) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(keys.get(i))
                    .build();

                var response = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .putObjectRequest(putObjectRequest)
                    .signatureDuration(PRESIGN_DURATION)
                    .build());

                presignedChunks.add(new PresignedChunk(i, response.url().toString(), response.expiration().getEpochSecond()));
            }

            return presignedChunks;

        } catch (Exception e) {
            logger.error("Error generating presigned chunks: {}", e.getMessage(), e);
            throw new PresignException(e.getMessage());
        }
    }

    private String resolveBucket(String bucket) {
        if ("raw".equals(bucket)) return rawBucket;
        if ("processed".equals(bucket)) return processedBucket;
        throw new IllegalArgumentException("Unknown bucket alias: " + bucket);
    }
}
