package com.shin.metadata.dto;

import java.time.LocalDateTime;

public record EncodeFinishedEvent(
        String videoId,
        String status,
        String[] resolutions,
        Double duration,
        Integer totalFiles,
        LocalDateTime timestamp
) {
}
