package com.shin.comment.domain.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Comment {

    private String id;
    private String createdAt;
    private String parentId;
    private String videoId;
    private String textOriginal;
    private String textDisplay;
    private Long likeCount;
    private boolean deleted;
    private String authorId;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String authorLink;
    private String updatedAt;

    public void applyLikeDelta(Long likeDelta) {
        this.likeCount = likeDelta + likeDelta;
    }
}