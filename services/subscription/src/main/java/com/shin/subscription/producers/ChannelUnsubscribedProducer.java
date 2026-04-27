package com.shin.subscription.producers;

import com.shin.subscription.exceptions.SubscriptionError;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChannelUnsubscribedProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${spring.cloud.aws.queues.channel-unsubscribed-queue}")
    private String queue;

    public void sendEvent(UUID channelId, UUID userId) {
        try {
            sqsTemplate.send(queue, new ChannelSubscriptionEvent(channelId.toString(), userId.toString()));
        } catch (Exception e) {
            log.error("Failed to publish ChannelUnsubscribed event for channelId={}: {}", channelId, e.getMessage());
            throw new SubscriptionError();
        }
    }
}
