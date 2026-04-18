package com.shin.metadata.service;

import com.shin.metadata.dto.VideoProgressResponse;
import com.shin.metadata.model.VideoProcessing;

import java.util.UUID;

public interface VideoProcessingService {

    VideoProcessing create(
        UUID videoId,
        String fileName,
        Long durationSeconds,
        Long fileSizeBytes,
        String fileType
    );

    VideoProcessing update(
            UUID videoId,
            String fileName,
            Long durationSeconds,
            Long fileSizeBytes,
            String fileType,
            Integer transcodingProgress,
            Integer uploadingProgress,
            String failureReason
    );

    VideoProgressResponse progress(
            UUID userId,
            UUID videoId
    );

    VideoProcessing findById(
            UUID videoId
    );

}
