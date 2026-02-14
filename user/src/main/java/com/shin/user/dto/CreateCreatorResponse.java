package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCreatorResponse(
        UUID id,
        String displayName,  // from User
        String email,        // from User
        String username,     // from Creator
        String channelUrl,   // from Creator
        String avatar,       // from Storage
        String banner,       // from Storage
        String languageTag,  // from User
        Boolean showAdultContent,  // from User
        LocalDateTime createdAt
) {
}
