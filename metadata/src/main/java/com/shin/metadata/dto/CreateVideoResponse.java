package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;

import java.util.UUID;

public record CreateVideoResponse(
    UUID id,
    String title,
    String description,
    VideoVisibility visibility,
    ProcessingStatus status,
    String resolutions
) {}
