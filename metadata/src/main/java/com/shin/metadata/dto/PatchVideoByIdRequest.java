package com.shin.metadata.dto;

import com.shin.metadata.model.VideoCategory;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Set;

public record PatchVideoByIdRequest(
    String title,
    String description,
    Double duration,
    String resolutions,
    String uploadKey,
    String thumbnailUrl,
    VideoCategory videoCategory,
    VideoVisibility visibility,
    VideoLanguage defaultLanguage,
    Boolean onlyForAdults,
    @Valid Set<TagIdentifier> tagsToAdd,
    @Valid Set<TagIdentifier> tagsToRemove,
    LocalDateTime scheduledPublishAt,
    ProcessingStatus status
) {}
