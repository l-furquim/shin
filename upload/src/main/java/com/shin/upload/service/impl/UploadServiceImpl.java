package com.shin.upload.service.impl;

import com.shin.upload.dto.*;
import com.shin.upload.exceptions.InvalidChunkException;
import com.shin.upload.exceptions.InvalidVideoUploadException;
import com.shin.upload.exceptions.UploadNotFoundException;
import com.shin.upload.model.UploadState;
import com.shin.upload.model.enums.UploadStatus;
import com.shin.upload.producers.EncodingJobProducer;
import com.shin.upload.producers.RawUploadMetadataProducer;
import com.shin.upload.producers.VideoInitializedProducer;
import com.shin.upload.service.StorageService;
import com.shin.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadServiceImpl implements UploadService {

    private static final long CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_RAW_SIZE = 100 * 1024 * 1024; // 100 MB

    private final StorageService storageService;
    private final VideoInitializedProducer videoInitializedProducer;
    private final EncodingJobProducer encodingProducer;
    private final RawUploadMetadataProducer rawUploadMetadataProducer;
    private final RedisTemplate<String, UploadState> redisTemplate;

    @Override
    public RawUploadResponse uploadRawVideo(String userId, RawUploadData data) {
        if(data.fileSize() > MAX_RAW_SIZE){
            throw new InvalidVideoUploadException("File too large for raw upload. Max size: 100MB");
        }

        UUID uploadId = UUID.randomUUID();

        if(data.fileName() == null || data.fileName().isBlank()) {
            throw new InvalidVideoUploadException("File name is invalid");
        }
        final var resolutions = sanitizeResolutions(data.resolutions());
        final var videoId = UUID.randomUUID();

        log.info("Initiating raw upload: uploadId={}, videoId={}", uploadId, videoId);

        final String finalKey = videoId + "/original.mp4";

        final var presignedUpload = this.storageService.generaePresignedUpload(
                "raw",
                finalKey,
                data.mimeType(),
                videoId.toString(),
                userId,
                data.fileName(),
                data.fileSize(),
                resolutions
        );

        rawUploadMetadataProducer.createEvent(
                new RawUploadCreatedEvent(
                        videoId.toString(),
                        userId,
                        finalKey,
                        data.fileName(),
                        String.join(",", resolutions),
                        data.mimeType(),
                        data.fileSize()
                )
        );

        return new RawUploadResponse(
                videoId.toString(),
                presignedUpload,
                "WAITING_UPLOAD"
        );
    }

    @Override
    public InitiateUploadResponse initiateUpload(String userId, InitiateUploadRequest request) {
        validateVideoFile(request.fileName(), request.fileSize(), request.contentType());
        final var resolutions = sanitizeResolutions(request.resolutions());

        UUID videoId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        long totalChunks = (request.fileSize() / CHUNK_SIZE) + (request.fileSize() % CHUNK_SIZE > 0 ? 1 : 0);

        log.info("Initiating upload: uploadId={}, videoId={}, totalChunks={}", uploadId, videoId, totalChunks);

        UploadState uploadState = new UploadState(
                uploadId,
                videoId,
                userId,
                request.fileName(),
                request.fileSize(),
                (int) totalChunks,
                0,
                String.join(",", resolutions),
                UploadStatus.INITIATED,
                LocalDateTime.now()
        );

        redisTemplate.opsForValue().set("upload:" + uploadId, uploadState);

        videoInitializedProducer.publish(new VideoInitializedEvent(
                videoId.toString(),
                userId,
                "Untitled",
                "PRIVATE",
                "UPLOADING",
                String.join(",", resolutions)
        ));

        return new InitiateUploadResponse(
            uploadId,
            videoId,
            CHUNK_SIZE,
            (int) totalChunks,
            resolutions
        );
    }

    @Override
    public ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, byte[] data) {
        UploadState state = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (state == null) {
            throw new UploadNotFoundException("Upload not found");
        }

        final int totalChunks = state.totalChunks();

        boolean result = true;
        double progress = (state.uploadedChunks().doubleValue() / state.totalChunks()) * 100;

        try {
            if (chunkNumber < 1 || chunkNumber > totalChunks) {
                throw new InvalidChunkException("Invalid chunk number. Expected 1 to " + totalChunks + ", got " + chunkNumber);
            }

            String chunkKey = state.videoId() + "/temp" + "/chunk-" + chunkNumber;
            storageService.upload("raw", chunkKey, data, "application/octet-stream");

            int newUploadedChunks = state.uploadedChunks() + 1;

            UploadState updatedState = new UploadState(
                state.id(),
                state.videoId(),
                state.userId(),
                state.fileName(),
                state.fileSize(),
                state.totalChunks(),
                newUploadedChunks,
                state.resolutions(),
                UploadStatus.UPLOADING,
                state.createdAt()
            );

            log.info("Uploaded chunks: {} of {} for uploadId={} in key={}", newUploadedChunks, state.totalChunks(), uploadId, chunkKey);

            redisTemplate.opsForValue().set("upload:" + uploadId, updatedState, Duration.ofHours(24));

            progress = (updatedState.uploadedChunks().doubleValue() / totalChunks) * 100;

        } catch (Exception e) {
            log.error("Error uploading chunk {} for uploadId={}: {}", chunkNumber, uploadId, e.getMessage(), e);
            result = false;
        }

        return new ChunkUploadResponse(uploadId, chunkNumber, result, progress);
    }

    @Override
    public RawUploadResponse completeUpload(String uploadId) {
        UploadState state = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (state == null) {
            throw new UploadNotFoundException("Upload not found");
        }

        if (!state.uploadedChunks().equals(state.totalChunks())) {
            throw new InvalidChunkException("Upload is incomplete. Uploaded " + state.uploadedChunks() + " of " + state.totalChunks() + " chunks");
        }

        UploadState processingState = new UploadState(
            state.id(),
            state.videoId(),
            state.userId(),
            state.fileName(),
            state.fileSize(),
            state.totalChunks(),
            state.uploadedChunks(),
            state.resolutions(),
            UploadStatus.COMPLETED,
            state.createdAt()
        );

        redisTemplate.opsForValue().set("upload:" + uploadId, processingState, Duration.ofHours(24));

        String finalKey = state.videoId() + "/original.mp4";

        assembleAndProcess(processingState, finalKey);

        final var presignedUpload = this.storageService.generaePresignedUpload("raw", finalKey, "video/mp4");

        return new RawUploadResponse(
            state.videoId().toString(),
            presignedUpload,
            "PROCESSING"
        );
    }

    @Override
    public CancelUploadResponse cancelUpload(String uploadId) {
        UploadState state = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (state == null) {
            throw new UploadNotFoundException("Upload not found");
        }

        try {
            List<String> keys = IntStream.rangeClosed(1, state.totalChunks())
                .mapToObj(chunkNumber -> state.videoId() + "/temp" + "/chunk-" + chunkNumber)
                .toList();

            storageService.deleteMultiple("raw", keys);
        } catch (Exception e) {
            log.error("Error while canceling the upload: ", e);
        }

        redisTemplate.delete("upload:" + uploadId);

        return new CancelUploadResponse(
            state.id().toString(),
            state.uploadedChunks(),
            state.totalChunks()
        );
    }

    private void assembleAndProcess(UploadState state, String finalKey) {
        List<String> chunks = IntStream.rangeClosed(1, state.totalChunks())
            .mapToObj(i -> state.videoId() + "/temp" + "/chunk-" + i)
            .toList();

        storageService.assembleChunks(chunks, "raw", finalKey, buildRawObjectMetadata(state, finalKey));
        chunks.forEach(chunkKey -> storageService.delete("raw", chunkKey));

        long processingTimeInMinutes = Duration.between(state.createdAt(), LocalDateTime.now()).toMinutes();
        log.info("Assembled chunks into final video at key={} for uploadId={} in {} minutes", finalKey, state.id(), processingTimeInMinutes);

        finishUpload(state, finalKey);
    }

    private void validateVideoFile(String fileName, Long fileSize, String contentType) {
        List<String> allowedExtensions = Arrays.asList("mp4", "webm", "mov", "avi", "mkv");
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        if (!allowedExtensions.contains(extension)) {
            throw new InvalidVideoUploadException("Invalid file type. Allowed: " + String.join(", ", allowedExtensions));
        }

        if (fileSize > 10L * 1024 * 1024 * 1024) {
            throw new InvalidVideoUploadException("File too large. Max size: 10GB");
        }

        if (!contentType.startsWith("video/")) {
            throw new InvalidVideoUploadException("Invalid content type. Must be video/*");
        }
    }

    private void finishUpload(UploadState state, String finalKey) {
        encodingProducer.createJob(
                new TranscodeJobEvent(
                        state.videoId().toString(),
                        finalKey,
                        state.userId(),
                        state.fileName(),
                        Arrays.asList(state.resolutions().split(","))
                )
        );

        redisTemplate.delete("upload:" + state.id());
    }

    private java.util.Map<String, String> buildRawObjectMetadata(UploadState state, String finalKey) {
        return java.util.Map.of(
                "videoid", state.videoId().toString(),
                "userid", state.userId(),
                "filename", state.fileName(),
                "resolutions", state.resolutions(),
                "s3key", finalKey,
                "contenttype", "video/mp4",
                "filesize", String.valueOf(state.fileSize())
        );
    }

    private List<String> sanitizeResolutions(String[] resolutions) {
        if (resolutions == null || resolutions.length == 0) {
            throw new InvalidVideoUploadException("Resolutions are required");
        }

        List<String> normalized = new ArrayList<>();
        for (String resolution : resolutions) {
            if (resolution != null && !resolution.isBlank()) {
                normalized.add(resolution.trim());
            }
        }

        if (normalized.isEmpty()) {
            throw new InvalidVideoUploadException("Resolutions are required");
        }

        return normalized;
    }
}
