package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InitVideoResponse(
        UUID videoId,
        ProcessingStatus status,
        LocalDateTime expiresAt
) {
}
