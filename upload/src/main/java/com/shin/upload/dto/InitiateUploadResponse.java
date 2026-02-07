package com.shin.upload.dto;

import java.util.List;
import java.util.UUID;

public record InitiateUploadResponse(
    UUID uploadId,
    UUID videoId,
    Long chunkSize,
    Integer totalChunks,
    List<String> resolutions
) {}
