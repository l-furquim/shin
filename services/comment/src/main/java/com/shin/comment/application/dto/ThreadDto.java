package com.shin.comment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ThreadDto", description = "Thread representation used in list responses")
public record ThreadDto(
        @Schema(description = "Top-level comment id used as thread identifier", example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea")
        String id,
        @Schema(description = "Video id associated with this thread", format = "uuid", example = "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b")
        String videoId,
        @Schema(description = "Channel id associated with this thread", format = "uuid", example = "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca")
        String channelId,
        @Schema(description = "Author id", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf")
        String authorId,
        @Schema(description = "Author display name", example = "Jane Doe")
        String authorDisplayName,
        @Schema(description = "Author avatar URL", nullable = true, example = "https://cdn.shin.com/avatar/jane.png")
        String authorAvatarUrl,
        @Schema(description = "Author profile URL", nullable = true, example = "https://shin.com/@janedoe")
        String authorUrl,
        @Schema(description = "Total number of replies in this thread", example = "4")
        Long totalReplyCount,
        @Schema(description = "Creation timestamp (ISO-8601 string)", example = "2026-04-11T11:33:45")
        String createdAt,
        @Schema(description = "Last update timestamp (ISO-8601 string)", example = "2026-04-11T11:50:00")
        String updatedAt
) {}
