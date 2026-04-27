package com.shin.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RawUploadData(
        @NotNull Long fileSize,
        @NotBlank String fileName,
        @NotBlank String mimeType,
        @NotEmpty String[] resolutions
) {}
