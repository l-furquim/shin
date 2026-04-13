package com.shin.comment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;
import java.util.UUID;

@Schema(name = "CreateCommentRequest", description = "Payload for creating a top-level comment or a reply")
public record CreateCommentRequest(
        @Schema(description = "Parent comment id. Omit for top-level comments.", nullable = true, example = "34dddacf-f148-4eea-bf56-a180fe8f77fe")
        Optional<String> parentId,
        @Schema(description = "Video id associated with the comment", format = "uuid", example = "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b")
        UUID videoId,
        @Schema(description = "Channel id associated with the comment thread", format = "uuid", example = "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca")
        UUID channelId,
        @Schema(description = "Raw comment text. Current sanitization rejects blank text and content longer than 1000 characters.", example = "Great video! Really helpful.")
        String content
) {
}
