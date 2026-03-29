package com.shin.metadata.dto;

import java.time.LocalDateTime;

public record EncodeFinishedEvent(
        String videoId,
        String status,
        String processedPath,
        String[] resolutions,
        Double duration,
        Integer totalFiles,
        String fileName,
        Long fileSize,
        String fileType,
        LocalDateTime timestamp
) {
}
