package com.shin.auth.dto;

public record AuthResponse(
        String token,
        Long expiresIn
   ) {
}
