package com.shin.metadata.dto;

import com.shin.metadata.model.enums.TranscodingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVideoRequest(
    @Nullable UUID videoId,
    @NotBlank String title,
    String description,
    @NotNull VideoVisibility visibility,
    @NotNull TranscodingStatus status,
    @NotBlank String accountId,
    @NotBlank String resolutions,
    @Nullable String s3Key,
    @Nullable String fileName,
    @Nullable Long fileSize,
    @Nullable String fileType
) {}
