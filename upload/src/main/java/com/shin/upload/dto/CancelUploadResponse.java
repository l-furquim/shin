package com.shin.upload.dto;

public record CancelUploadResponse(
    String id,
    Integer uploadedChunks,
    Integer totalChunks
) {}
