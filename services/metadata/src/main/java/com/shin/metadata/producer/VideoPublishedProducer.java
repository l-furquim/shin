package com.shin.metadata.producer;

import com.shin.metadata.dto.VideoPublishedEvent;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class VideoPublishedProducer {

    @Value("${spring.cloud.aws.sns.video-published-topic-arn}")
    private String topicName;

    private final SnsTemplate snsTemplate;

    public void sendEvent(VideoPublishedEvent event) {
        try {
            SnsNotification<VideoPublishedEvent> notification = SnsNotification.builder(event).build();

           snsTemplate.sendNotification(this.topicName, notification);
        } catch (Exception e) {
            log.error("Error while sending the video published message topic: {}", e.getMessage());
        }
    }

}
