package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;
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
    @NotNull ProcessingStatus status,
    @NotBlank String accountId,
    @NotBlank String resolutions
) {}
