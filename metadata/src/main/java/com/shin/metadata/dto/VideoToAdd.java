package com.shin.metadata.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VideoToAdd(
    @NotNull UUID videoId,
    Integer position
) {}
