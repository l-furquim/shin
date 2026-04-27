package com.shin.search.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record VideoDto(
        UUID id,
        String title,
        String description,
        String visibility,
        String categoryId,
        Map<String, Thumbnail> thumbnails,
        ContentDetails contentDetails,
        Statistics statistics,
        Boolean likedByMe,
        FileDetails fileDetails,
        Channel channel,
        Set<String> tags,
        LocalDateTime publishedAt,
        LocalDateTime scheduledPublishAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
