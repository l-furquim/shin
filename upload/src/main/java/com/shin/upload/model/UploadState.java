package com.shin.upload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shin.upload.model.enums.UploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UploadState(
    @JsonProperty("id") UUID id,
    @JsonProperty("videoId") UUID videoId,
    @JsonProperty("userId") String userId,
    @JsonProperty("fileName") String fileName,
    @JsonProperty("fileSize") Long fileSize,
    @JsonProperty("totalChunks") Integer totalChunks,
    @JsonProperty("uploadedChunks") Integer uploadedChunks,
    @JsonProperty("resolutions") String resolutions,
    @JsonProperty("status") UploadStatus status,
    @JsonProperty("createdAt") LocalDateTime createdAt
) {
    @JsonCreator
    public UploadState {}
}
