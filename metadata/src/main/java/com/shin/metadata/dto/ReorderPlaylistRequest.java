package com.shin.metadata.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record ReorderPlaylistRequest(
    @NotEmpty Set<UUID> videosIds
) {}
