package com.shin.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public record ChunkedUploadRequest(
    @NotBlank String fileName,
    @NotNull Long fileSize,
    @NotBlank String mimeType,
    @NotEmpty String[] resolutions
) {}
