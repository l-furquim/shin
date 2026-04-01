package com.shin.upload.dto;

public record RawUploadCreatedEvent(
        String videoId,
        String userId,
        String s3Key,
        String fileName,
        String resolutions,
        String contentType,
        Long fileSize
) {
}
