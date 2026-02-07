package com.shin.upload.service.impl;

import com.shin.upload.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
public class S3StorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client client;

    @Value("${spring.cloud.aws.s3.raw-bucket-url}")
    private String rawBucket;

    @Value("${spring.cloud.aws.s3.processed-bucket-url}")
    private String processedBucket;

    public S3StorageServiceImpl(S3Client client) {
        this.client = client;
    }

    @Override
    public void upload(String bucket, String key, byte[] data, String contentType) {
        try {
            String resolvedBucket = resolveBucket(bucket);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolvedBucket)
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
            logger.info("Uploading assembled video to S3: {}", destKey);

            String resolvedDestBucket = resolveBucket(destBucket);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(resolvedDestBucket)
                .key(destKey)
                .contentType("video/mp4")
                .build();

            client.putObject(putRequest, RequestBody.fromFile(outputFile));

            logger.info("Successfully uploaded assembled video to S3: {}", destKey);

        } catch (Exception e) {
            logger.error("Error assembling chunks: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            logger.info("Cleaning up temporary files");
            for (File chunk : chunks) {
                try {
                    if (chunk.delete()) {
                        logger.debug("Deleted chunk file: {}", chunk.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to delete chunk file: {}, {}", chunk.getAbsolutePath(), e.getMessage());
                }
            }
            if (outputFile != null) {
                try {
                    if (outputFile.delete()) {
                        logger.debug("Deleted output file: {}", outputFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to delete output file: {}, {}", outputFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            String resolvedBucket = resolveBucket(bucket);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(resolvedBucket)
                .key(key)
                .build();

            client.deleteObject(deleteRequest);
        } catch (Exception e) {
            logger.error("Error deleting from S3: {}", e.getMessage(), e);
        }
    }

    @Override
    public void deleteMultiple(String bucket, List<String> keys) {
        try {
            String resolvedBucket = resolveBucket(bucket);

            for (String key : keys) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(key)
                    .build();

                client.deleteObject(deleteRequest);
            }
        } catch (Exception e) {
            logger.error("Error deleting from S3: {}", e.getMessage(), e);
        }
    }

    private String resolveBucket(String bucket) {
        if ("raw".equals(bucket)) {
            return rawBucket;
        } else if ("processed".equals(bucket)) {
            return processedBucket;
        } else {
            throw new IllegalArgumentException("Invalid bucket name");
        }
    }
}
