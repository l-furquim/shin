package com.shin.upload.dto;

public record ThumbnailJobEvent(
        String videoId,
        String s3Key
) {
}
