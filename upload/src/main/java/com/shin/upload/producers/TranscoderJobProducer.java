package com.shin.upload.producers;

import com.shin.upload.dto.TranscodeJobEvent;
import com.shin.upload.exceptions.TranscoderProducerException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TranscoderJobProducer {

    private final SqsTemplate template;

    @Value("${spring.cloud.aws.queues.encode-job-queue}")
    private String queue;

    public void createJob(TranscodeJobEvent event) {
        try {
            template.send(queue, event);
        } catch (Exception e) {
            throw new TranscoderProducerException(
                e.getMessage() != null ? e.getMessage() : "Error while sending message to queue"
            );
        }
    }
}
