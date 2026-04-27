package com.shin.auth.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String encryptedPassword
) {
}
