package com.shin.auth.dto;

public record TokenResponse(
        String token,
        String deviceId,
        String refreshToken,
        Long tokenExpiresIn
) {
}
