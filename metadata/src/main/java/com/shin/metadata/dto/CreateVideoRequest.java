package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateVideoRequest(
    @NotNull UUID videoId,
    @NotBlank String title,
    String description,
    @NotNull VideoVisibility visibility,
    @NotNull ProcessingStatus status,
    @NotBlank String accountId,
    @NotEmpty List<String> resolutions
) {}
