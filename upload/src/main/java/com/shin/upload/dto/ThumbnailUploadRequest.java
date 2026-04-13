package com.shin.upload.dto;

import java.util.UUID;

public record ThumbnailUploadRequest(
        UUID videoId,
        String contentType,
        Long fileSize
) {
}
