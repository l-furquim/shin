package com.shin.streaming.dto;

import java.util.UUID;

public record VideoDetails(
        UUID id,
        UUID creatorId,
        String title,
        String description,
        String visibility,
        String status
) {
}
