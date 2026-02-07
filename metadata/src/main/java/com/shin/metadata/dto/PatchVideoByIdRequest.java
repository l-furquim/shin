package com.shin.metadata.dto;

import com.shin.metadata.model.VideoCategory;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record PatchVideoByIdRequest(
    String title,
    String description,
    Long duration,
    List<String> resolutions,
    String videoKey,
    String thumbnailUrl,
    VideoCategory videoCategory,
    VideoVisibility visibility,
    VideoLanguage defaultLanguage,
    Boolean onlyForAdults,
    List<String> tags,
    LocalDateTime scheduledPublishAt,
    ProcessingStatus status
) {}
