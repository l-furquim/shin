package com.shin.metadata.dto;

import com.shin.metadata.model.VideoCategory;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoLanguage;
import com.shin.metadata.model.enums.VideoVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetVideoByIdResponse(
    UUID id,
    UUID videoId,
    String title,
    String description,
    VideoVisibility visibility,
    String accountId,
    Boolean onlyForAdults,
    String videoKey,
    String thumbnailUrl,
    VideoCategory videoCategory,
    VideoLanguage defaultLanguage,
    String publishedLocale,
    List<String> tags,
    Long duration,
    List<String> resolutions,
    LocalDateTime publishedAt,
    LocalDateTime scheduledPublishAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ProcessingStatus status
) {}
