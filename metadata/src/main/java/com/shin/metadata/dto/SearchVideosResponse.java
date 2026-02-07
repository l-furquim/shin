package com.shin.metadata.dto;

import java.util.List;

public record SearchVideosResponse(
    List<VideoDto> videos
) {}
