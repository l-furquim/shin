package com.shin.upload.dto;

public record ChunkUploadResponse(
    String uploadId,
    Integer chunkNumber,
    Boolean uploaded,
    Double progress
) {}
