package com.shin.upload.dto;

public record ThumbnailUploadResponse(
        String videoId,
        PresignedUpload upload
) {
}
