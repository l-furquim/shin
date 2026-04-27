package com.shin.streaming.model;

import com.shin.commons.annotations.TableName;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
@TableName(name = "playback_sessions")
public class PlaybackSession {

     private String sessionId;
     private long accumulatedWatchSeconds;
     private long expiresAt;
     private String userId;
     private String videoId;
     private boolean viewCounted;

     @DynamoDbPartitionKey
     public String getSessionId() {
        return sessionId;
     }

}
