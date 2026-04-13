package com.shin.comment.infrastructure.entity;

import com.shin.commons.annotations.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@TableName(name="comment_threads")
@DynamoDbBean
public class ThreadEntity {

    private String videoId;
    private String topLevelCommentId;

    @DynamoDbPartitionKey
    public String getVideoId() {
        return this.videoId;
    }

    @DynamoDbSortKey
    public String getTopLevelCommentId() {
        return this.topLevelCommentId;
    }

    private String channelId;
    private String authorId;

    private String authorDisplayName;
    private String authorAvatarUrl;
    private String authorLink;

    private Long totalReplyCount;
    private String createdAt;
    private String updatedAt;


}
