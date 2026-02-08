package com.shin.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InitiateUploadRequest(
    @NotBlank String videoId,
    @NotBlank String fileName,
    @NotNull Long fileSize,
    @NotBlank String contentType,
    @NotEmpty List<String> resolutions
) {}
