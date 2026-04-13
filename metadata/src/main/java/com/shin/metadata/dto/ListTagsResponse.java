package com.shin.metadata.dto;

import java.util.List;

public record ListTagsResponse(
        List<TagDto> items
) {}

