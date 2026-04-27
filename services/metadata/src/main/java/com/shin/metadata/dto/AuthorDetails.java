package com.shin.metadata.dto;

import java.util.UUID;

public record AuthorDetails(
        String authorDisplayName,
        String authorImageUrl,
        UUID authorChannelId
) {
}
