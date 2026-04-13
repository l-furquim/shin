package com.shin.comment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCommentResponse", description = "Comment data returned after an update operation")
public record UpdateCommentResponse(
        @Schema(description = "Comment id", example = "34dddacf-f148-4eea-bf56-a180fe8f77fe")
        String id,
        @Schema(description = "Thread id. For top-level comments, this value matches id.", example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea")
        String threadId,
        @Schema(description = "Parent comment id when this comment is a reply", nullable = true, example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea")
        String parentId,
        @Schema(description = "Video id associated with this comment", format = "uuid", example = "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b")
        String videoId,
        @Schema(description = "Author id", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf")
        String authorId,
        @Schema(description = "Sanitized plain content", example = "Updated content")
        String textOriginal,
        @Schema(description = "Formatted display content (HTML-safe)", example = "Updated content")
        String textDisplay,
        @Schema(description = "Current like count", example = "10")
        Long likeCount
) {
}
