package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateUserResponse(
        UUID id,
        String displayName,
        String email,
        Boolean showAdultContent,
        String locale,
        LocalDateTime updatedAt,
        LocalDateTime createdAt
) {
}
