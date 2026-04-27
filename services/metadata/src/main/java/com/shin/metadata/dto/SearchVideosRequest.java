package com.shin.metadata.dto;

import java.util.UUID;

public record SearchVideosRequest(
        String id,
        String ids,
        UUID channelId,
        String fields,
        String myRating,
        boolean forMine,
        String categoryId,
        String cursor,
        int limit
) {
}
