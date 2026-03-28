package com.shin.upload.dto;

public record RawUploadData(
        String videoId,
        String[] resolutions
) {
}
