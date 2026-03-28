package com.shin.metadata.dto;

public record SearchVideosRequest(
        String id,
        String fields,
        String myRating,
        String categoryId,
        String cursor,
        int limit
) {
}
