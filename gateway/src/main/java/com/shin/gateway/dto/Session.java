package com.shin.gateway.dto;

import java.time.LocalDateTime;

public record Session(
        String userId,
        String deviceId,
        String ip,
        String refreshToken,
        String accessToken,
        boolean revoked,
        String userAgent,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
}
