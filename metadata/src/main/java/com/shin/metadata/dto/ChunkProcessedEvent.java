package com.shin.metadata.dto;

import java.time.LocalDateTime;

public record ChunkProcessedEvent(
        String videoId,
        int progress,
        String status,
        String resolution,
        int filesUploaded,
        int totalFiles,
        LocalDateTime timestamp
) {
}
