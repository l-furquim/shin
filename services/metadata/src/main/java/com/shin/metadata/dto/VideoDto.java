package com.shin.metadata.dto;

import com.shin.metadata.model.enums.VideoVisibility;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record VideoDto(
    UUID id,
    String title,
    String description,
    VideoVisibility visibility,
    String categoryId,
    Map<String, Thumbnail>  thumbnails,
    ContentDetails contentDetails,
    Statistics statistics,
    Boolean likedByMe,
    FileDetails fileDetails,
    ProcessingDetails processingDetails,
    Channel channel,
    Set<String> tags,
    LocalDateTime publishedAt,
    LocalDateTime scheduledPublishAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
