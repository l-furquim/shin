package com.shin.search.dto;

import java.util.List;

public record VideoDto(
        String id,
        String title,
        String description,
        String categoryName,
        String channelName,
        String channelAvatar,
        Double duration,
        String thumbnailUrl,
        String videoLink,
        String language,
        boolean forAdults,
        List<String> tags,
        Double score,
        String publishedAt
) {
}
