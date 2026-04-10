package com.shin.comment.model;

import com.shin.commons.annotations.TableName;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@DynamoDbBean
@TableName(name= "comments")
public class Comment {

    private String id;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getId() {
       return this.id;
    }

    @DynamoDbSortKey
    public String getCreatedAt() {
        return this.createdAt;
    }

    @Getter
    private String parentId;

    @Getter
    private String videoId;


    @Setter
    @Getter
    private String textOriginal;

    @Setter
    @Getter
    private String textDisplay;

    @Getter
    private Long likeCount;

    @Setter
    @Getter
    private boolean deleted;


    @Getter
    private String authorId;

    @Getter
    private String authorDisplayName;

    @Getter
    private String authorAvatarUrl;

    @Getter
    private String authorLink;

    @Setter
    @Getter
    private String updatedAt;

    public void applyLikeDelta(Long likeDelta){
        this.likeCount = likeDelta + likeDelta;
    }
}
