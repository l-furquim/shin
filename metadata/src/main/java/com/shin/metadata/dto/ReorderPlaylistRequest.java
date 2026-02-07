package com.shin.metadata.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderPlaylistRequest(
    @NotEmpty List<UUID> videosIds
) {}
