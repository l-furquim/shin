package com.shin.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetCreatorByIdResponse(
        UUID id,
        String displayName,
        String username,
        String description,
        String channelUrl,
        String avatar,
        String banner,
        String languageTag,
        LocalDateTime createdAt
) {
}
