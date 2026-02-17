package com.shin.gateway.dto;

public record AuthResponse(
        String token,
        Long tokenExpiresIn
) {
}
