package com.shin.comment.dto;

import java.util.UUID;

public record CreatorResponse(
        UUID id,
        String displayName,
        String username,
        String avatar,
        String banner,
        String link
) {
}
