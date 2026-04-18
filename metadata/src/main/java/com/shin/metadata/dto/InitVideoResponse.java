package com.shin.metadata.dto;

import com.shin.metadata.model.enums.TranscodingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InitVideoResponse(
        UUID videoId,
        TranscodingStatus status,
        LocalDateTime expiresAt
) {
}
