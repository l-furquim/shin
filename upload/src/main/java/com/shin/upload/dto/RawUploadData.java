package com.shin.upload.dto;

public record RawUploadData(
        String[] resolutions,
        String videoId
) {
}
