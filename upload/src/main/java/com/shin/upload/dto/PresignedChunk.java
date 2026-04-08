package com.shin.upload.dto;

public record PresignedChunk(int chunkIndex, String url, Long expiresAt) {}
