package com.shin.upload.service.impl;

import com.shin.upload.dto.*;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadServiceImpl implements UploadService {

    private static final long CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_RAW_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final Duration UPLOAD_STATE_TTL = Duration.ofHours(24);

    private final StorageService storageService;
    private final VideoInitializedProducer videoInitializedProducer;
    private final EncodingJobProducer encodingProducer;
    private final RawUploadMetadataProducer rawUploadMetadataProducer;
    private final RedisTemplate<String, UploadState> redisTemplate;

    @Override
    public RawUploadResponse initiateRawUpload(String userId, RawUploadData data) {
        if (data.fileSize() > MAX_RAW_SIZE) {
            throw new InvalidVideoUploadException("File too large for raw upload. Max size: 100MB");
        }

        final var resolutions = sanitizeResolutions(data.resolutions());
        final var videoId = UUID.randomUUID();
        final String finalKey = videoId + "/original.mp4";

        log.info("Initiating raw upload: videoId={}", videoId);

        final var presignedUpload = storageService.generatePresignedUpload(
            "raw",
            finalKey,
            data.mimeType(),
            videoId.toString(),
            userId,
            data.fileName(),
            data.fileSize(),
            resolutions
        );

        rawUploadMetadataProducer.send(new RawUploadCreatedEvent(
            videoId.toString(),
            userId,
            finalKey,
            data.fileName(),
            String.join(",", resolutions),
            data.mimeType(),
            data.fileSize()
        ));

        return new RawUploadResponse(videoId.toString(), presignedUpload, UploadStatus.INITIATED.name());
    }

    @Override
    public ChunkedUploadResponse initiateUpload(String userId, ChunkedUploadRequest request) {
        validateVideoFile(request.fileName(), request.fileSize(), request.contentType());
        final var resolutions = sanitizeResolutions(request.resolutions());

        UUID videoId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        long totalChunks = (request.fileSize() / CHUNK_SIZE) + (request.fileSize() % CHUNK_SIZE > 0 ? 1 : 0);

        log.info("Initiating chunked upload: uploadId={}, videoId={}, totalChunks={}", uploadId, videoId, totalChunks);

        UploadState uploadState = new UploadState(
            uploadId,
            videoId,
            userId,
            request.fileName(),
            request.fileSize(),
            (int) totalChunks,
            String.join(",", resolutions),
            UploadStatus.UPLOADING,
            LocalDateTime.now()
        );

        redisTemplate.opsForValue().set("upload:" + uploadId, uploadState, UPLOAD_STATE_TTL);

        List<String> keys = new ArrayList<>((int) totalChunks);
        for (int i = 0; i < totalChunks; i++) {
            keys.add(videoId + "/temp/chunk-" + i);
        }

        List<PresignedChunk> chunks = storageService.generatePresignedChunks("raw", keys);

        videoInitializedProducer.send(new VideoInitializedEvent(
            videoId.toString(),
            userId,
            "Untitled",
            "PRIVATE",
            UploadStatus.UPLOADING.name(),
            String.join(",", resolutions)
        ));

        return new ChunkedUploadResponse(videoId, chunks);
    }

    @Override
    public void completeUpload(String uploadId) {
        UploadState state = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (state == null) {
            throw new UploadNotFoundException("Upload not found");
        }

        String finalKey = state.videoId() + "/original.mp4";

        log.info("Completing chunked upload: uploadId={}, videoId={}", uploadId, state.videoId());

        assembleAndProcess(state, finalKey);
    }

    @Override
    public void cancelUpload(String uploadId) {
        UploadState state = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (state == null) {
            throw new UploadNotFoundException("Upload not found");
        }

        try {
            List<String> keys = IntStream.range(0, state.totalChunks())
                .mapToObj(i -> state.videoId() + "/temp/chunk-" + i)
                .toList();

            storageService.deleteMultiple("raw", keys);
        } catch (Exception e) {
            log.error("Error deleting chunks for uploadId={}: {}", uploadId, e.getMessage(), e);
        }

        redisTemplate.delete("upload:" + uploadId);

        log.info("Canceled upload: uploadId={}, videoId={}", uploadId, state.videoId());
    }

    private void assembleAndProcess(UploadState state, String finalKey) {
        List<String> chunks = IntStream.range(0, state.totalChunks())
            .mapToObj(i -> state.videoId() + "/temp/chunk-" + i)
            .toList();

        storageService.assembleChunks(chunks, "raw", finalKey, buildRawObjectMetadata(state, finalKey));
        chunks.forEach(key -> storageService.delete("raw", key));

        long elapsed = Duration.between(state.createdAt(), LocalDateTime.now()).toMinutes();
        log.info("Assembled {} chunks into key={} for uploadId={} in {} minutes",
            chunks.size(), finalKey, state.id(), elapsed);

        finishUpload(state, finalKey);
    }

    private void finishUpload(UploadState state, String finalKey) {
        encodingProducer.send(new TranscodeJobEvent(
            state.videoId().toString(),
            finalKey,
            state.userId(),
            state.fileName(),
            Arrays.asList(state.resolutions().split(","))
        ));

        redisTemplate.delete("upload:" + state.id());
    }

    private Map<String, String> buildRawObjectMetadata(UploadState state, String finalKey) {
        return Map.of(
            "videoid", state.videoId().toString(),
            "userid", state.userId(),
            "filename", state.fileName(),
            "resolutions", state.resolutions(),
            "s3key", finalKey,
            "contenttype", "video/mp4",
            "filesize", String.valueOf(state.fileSize())
        );
    }

    private void validateVideoFile(String fileName, Long fileSize, String contentType) {
        List<String> allowedExtensions = List.of("mp4", "webm", "mov", "avi", "mkv");
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
