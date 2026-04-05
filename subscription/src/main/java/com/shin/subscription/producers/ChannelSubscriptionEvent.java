package com.shin.subscription.producers;

public record ChannelSubscriptionEvent(String channelId, String userId) {
}
