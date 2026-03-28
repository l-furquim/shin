package com.shin.metadata.dto;

public record ThumbnailGeneratedEvent(
        String videoId,
        String s3Key,
        String timestamp
) {
}
