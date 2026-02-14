package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateUserResponse(
    UUID id,
    String displayName,
    String email,
    Boolean showAdultContent,
    String locale,
    String avatar,
    String banner,
    LocalDateTime updatedAt,
    LocalDateTime createdAt
) {
}
