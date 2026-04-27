package com.shin.auth.dto;

public record AuthResponse(
        String token,
        String deviceId,
        Long expiresIn
   ) {
}
