package com.shin.upload.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateVideoRequest(
    String title,
    String description,
    String playlistId,
    String videoKey,
    String thumbnailUrl,
    String videoCategory,
    String visibility,
    String defaultLanguage,
    Boolean onlyForAdults,
    List<String> tags,
    Long duration,
    List<String> resolutions,
    LocalDateTime scheduledPublishAt,
    String status
) {}
