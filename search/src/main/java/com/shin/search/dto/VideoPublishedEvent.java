package com.shin.search.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors metadata's VideoPublishedEvent, received via SNS → SQS.
 * Uses Jackson @JsonIgnoreProperties to be tolerant of unknown fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoPublishedEvent(
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
        String publishedAt,
        String visibility
) {
}
