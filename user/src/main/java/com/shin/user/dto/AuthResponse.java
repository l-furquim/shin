package com.shin.user.dto;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String encryptedPassword
) {
}
