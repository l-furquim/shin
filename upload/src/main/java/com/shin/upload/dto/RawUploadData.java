package com.shin.upload.dto;

public record RawUploadData(
        Long fileSize,
        String fileName,
        String mimeType,
        String[] resolutions
) {
}
