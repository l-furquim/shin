package com.shin.upload.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InitiateUploadRequest(
    @NotBlank String fileName,
    @NotNull Long fileSize,
    @NotBlank String contentType,
    @NotBlank String userId,
    @NotEmpty List<String> resolutions
) {}
