package com.shin.metadata.dto;

import com.shin.metadata.model.enums.VideoLanguage;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Set;

public record PatchVideoByIdRequest(
    String title,
    String description,
    Long duration,
    String resolutions,
    String uploadKey,
    String thumbnailUrl,
    Long categoryId,
    VideoLanguage defaultLanguage,
    Boolean onlyForAdults,
    @Valid Set<TagIdentifier> tagsToAdd,
    @Valid Set<TagIdentifier> tagsToRemove,
    LocalDateTime scheduledPublishAt
) {}
