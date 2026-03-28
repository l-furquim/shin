package com.shin.metadata.dto;

import java.util.UUID;

public record SearchCommentRequest(
        String fields,
        UUID id,
        UUID parentId,
        Long maxResults,
        String pageToken,
        String textFormat
) {
}
