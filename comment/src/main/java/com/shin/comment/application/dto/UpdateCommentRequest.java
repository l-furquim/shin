package com.shin.comment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCommentRequest", description = "Payload for updating comment content and like count delta")
public record UpdateCommentRequest(
        @Schema(description = "New comment content. When provided, it is sanitized and formatted before storage.", example = "Updated content")
        String content,
        @Schema(description = "Like count delta. Current implementation accepts null; non-null values may be rejected depending on validation path.", nullable = true, example = "1")
        Long likeDelta
) {
}
