package com.shin.interaction.producers;

public record VideoReactionEvent(String videoId, String userId, String reactionType, String action) {
}
