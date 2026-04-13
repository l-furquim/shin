package com.shin.comment.domain.model;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Thread {

    private String videoId;
    private String topLevelCommentId;
    private String channelId;
    private Long totalReplyCount;
    private String createdAt;
    private String updatedAt;

    private String authorId;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String authorLink;
}
