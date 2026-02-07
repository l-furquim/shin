package com.shin.metadata.dto;

import com.shin.metadata.model.enums.PlaylistVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePlaylistRequest(
    @NotBlank String title,
    String description,
    @NotNull PlaylistVisibility visibility,
    List<UUID> videos
) {}
