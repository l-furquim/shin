package com.shin.comment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentDto", description = "Comment representation used in list responses")
public record CommentDto(
        @Schema(description = "Comment id", example = "34dddacf-f148-4eea-bf56-a180fe8f77fe")
        String id,
        @Schema(description = "Parent comment id when this comment is a reply", nullable = true, example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea")
        String parentId,
        @Schema(description = "Video id associated with this comment", format = "uuid", example = "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b")
        String videoId,
        @Schema(description = "Author id", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf")
        String authorId,
        @Schema(description = "Author display name", example = "Jane Doe")
        String authorDisplayName,
        @Schema(description = "Author avatar URL", nullable = true, example = "https://cdn.shin.com/avatar/jane.png")
        String authorAvatarUrl,
        @Schema(description = "Author profile URL", nullable = true, example = "https://shin.com/@janedoe")
        String authorUrl,
        @Schema(description = "Display text. May be plain text or HTML depending on requested textFormat.", example = "Great point")
        String textDisplay,
        @Schema(description = "Original sanitized text", example = "Great point")
        String textOriginal,
        @Schema(description = "Current like count", example = "12")
        Long likeCount,
        @Schema(description = "Creation timestamp (ISO-8601 string)", example = "2026-04-11T11:33:45")
        String createdAt,
        @Schema(description = "Last update timestamp (ISO-8601 string)", example = "2026-04-11T11:50:00")
        String updatedAt
) {}
