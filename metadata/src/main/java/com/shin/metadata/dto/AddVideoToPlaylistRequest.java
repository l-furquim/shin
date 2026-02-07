package com.shin.metadata.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddVideoToPlaylistRequest(
    @NotEmpty List<VideoToAdd> videos
) {}
