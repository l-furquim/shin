package com.shin.interaction.model;

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
@TableName(name = "video_reaction_counters")
public class ReactionCount {

    private String videoId;

    private Long likesCount;

    private Long deslikesCount;

    @DynamoDbPartitionKey
    public String getVideoId() {
        return this.videoId;
    }
}
