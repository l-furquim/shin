package com.shin.interaction.model;

import com.shin.interaction.annotations.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
@TableName(name = "video_reaction_counters")
public class ReactionCount {

    private String videoId;

    @Getter
    private Long likesCount;

    @Getter
    private Long deslikesCount;

    @DynamoDbPartitionKey
    public String getVideoId() {
        return this.videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public void setDeslikesCount(Long deslikesCount) {
        this.deslikesCount = deslikesCount;
    }

    public Long getLikesCount() {
        return this.likesCount;
    }

    public Long getDeslikesCount() {
        return this.deslikesCount;
    }

}
