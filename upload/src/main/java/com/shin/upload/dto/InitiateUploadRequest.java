package com.shin.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public record InitiateUploadRequest(
    @NotBlank String fileName,
    @NotNull Long fileSize,
    @NotBlank String contentType,
    @NotEmpty String[] resolutions
) {}
