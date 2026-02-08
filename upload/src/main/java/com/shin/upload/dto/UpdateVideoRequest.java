package com.shin.upload.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UpdateVideoRequest(
    String title,
    String description,
    Long duration,
    String resolutions,
    String uploadKey,
    String thumbnailUrl,
    Integer videoCategoryId,
    String visibility,
    String defaultLanguage,
    Boolean onlyForAdults,
    Set<TagIdentifier> tagsToAdd,
    Set<TagIdentifier> tagsToRemove,
    LocalDateTime scheduledPublishAt,
    String status
) {}
