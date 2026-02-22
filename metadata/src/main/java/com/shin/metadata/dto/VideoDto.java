package com.shin.metadata.dto;

import com.shin.metadata.model.Tag;
import com.shin.metadata.model.VideoCategory;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record VideoDto(
    UUID id,
    String title,
    String description,
    VideoVisibility visibility,
    String creatorId,
    Boolean onlyForAdults,
    String uploadKey,
    String thumbnailUrl,
    VideoCategory videoCategory,
    VideoLanguage defaultLanguage,
    String publishedLocale,
    Set<Tag> tags,
    Double duration,
    String resolutions,
    Long likeCount,
    LocalDateTime publishedAt,
    LocalDateTime scheduledPublishAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ProcessingStatus status
) {}
