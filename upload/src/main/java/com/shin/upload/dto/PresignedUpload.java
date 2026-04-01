package com.shin.upload.dto;

public record PresignedUpload(
        String url,
        Long expiresAt
) {
}
