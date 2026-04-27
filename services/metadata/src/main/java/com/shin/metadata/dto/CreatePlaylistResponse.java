package com.shin.metadata.dto;

import com.shin.metadata.model.enums.PlaylistVisibility;

import java.util.List;
import java.util.UUID;

public record CreatePlaylistResponse(
    UUID id,
    String name,
    String description,
    String thumbnailUrl,
    PlaylistVisibility visibility,
    List<UUID> videos
) {}
