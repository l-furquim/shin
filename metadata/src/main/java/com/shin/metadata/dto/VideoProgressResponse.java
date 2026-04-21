package com.shin.metadata.dto;

import java.time.LocalDateTime;

public record VideoProgressResponse(
        Integer transcodingProgress,
        String failureReason,
        String transcodingStatus,
        Long fileSizeBytes,
        LocalDateTime startedAt
) {
}
