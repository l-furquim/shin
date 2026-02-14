package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetCreatorByIdResponse(
        UUID id,
        String displayName,  // from User
        String username,     // from Creator
        String description,  // from Creator
        String channelUrl,   // from Creator
        String avatar,       // from Storage
        String banner,       // from Storage
        String languageTag,  // from User
        LocalDateTime createdAt  // from Creator
) {
}
