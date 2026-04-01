package com.shin.upload.dto;

public record RawUploadResponse(
    String videoId,
    PresignedUpload upload,
    String status
) {
}
