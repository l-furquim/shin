package com.shin.subscription.producers;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ChannelSubscribedProducer {

    private final SqsTemplate sqsTemplate;

    // TODO: implement this
    public void sendEvent(){

    }

}
