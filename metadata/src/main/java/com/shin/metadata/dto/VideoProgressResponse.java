package com.shin.metadata.dto;

import java.time.LocalDateTime;

public record VideoProgressResponse(
        Integer transcodingProgress,
        Integer uploadingProgress,
        String failureReason,
        String transcodingStatus,
        String uploadingStatus,
        Long fileSizeBytes,
        LocalDateTime startedAt
) {
}
