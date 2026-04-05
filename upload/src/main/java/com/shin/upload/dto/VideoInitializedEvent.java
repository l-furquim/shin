package com.shin.upload.dto;

public record VideoInitializedEvent(
        String videoId,
        String userId,
        String title,
        String visibility,
        String status,
        String resolutions
) {
}
