package com.shin.auth.dto;

public record TokenResponse(
        String token,
        String refreshToken,
        Long tokenExpiresIn
) {
}
