package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;

import java.util.UUID;

public record WatchVideoResponse(
        UUID id,
        UUID creatorId,
        String title,
        Long duration,
        String description,
        VideoVisibility visibility,
        ProcessingStatus status
) {
}
