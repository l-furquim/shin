package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetMeResponse(
        UUID id,
        String displayName,
        String username,
        String email,
        boolean showAdultContent,
        String locale,
        String description,
        String channelUrl,
        String avatar,
        String banner,
        String languageTag,
        LocalDateTime createdAt,
        LocalDateTime lastUpdate
) {
}
