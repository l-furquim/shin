package com.shin.comment.dto;

import java.util.Optional;
import java.util.UUID;

public record CreateCommentRequest(
        Optional<String> parentId,
        UUID videoId,
        UUID channelId,
        String content
) {
}
