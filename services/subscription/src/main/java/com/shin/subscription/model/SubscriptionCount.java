package com.shin.subscription.model;

import com.shin.commons.annotations.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
@TableName(name = "channel_subscription_counters")
public class SubscriptionCount {

    private String channelId;

    private Long subscribersCount;

    @DynamoDbPartitionKey
    public String getChannelId() {
        return this.channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Long getSubscribersCount() {
        return this.subscribersCount;
    }

    public void setSubscribersCount(Long subscribersCount) {
        this.subscribersCount = subscribersCount;
    }

}
