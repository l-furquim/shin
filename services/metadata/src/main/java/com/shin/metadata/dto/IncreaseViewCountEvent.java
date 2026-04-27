package com.shin.metadata.dto;

import java.util.UUID;

public record IncreaseViewCountEvent(
        UUID videoId,
        UUID userId,
        String viewerKey
) {
}
