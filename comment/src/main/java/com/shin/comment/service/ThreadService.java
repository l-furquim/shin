package com.shin.comment.service;

import com.shin.comment.dto.ThreadListResponse;

import java.util.List;

public interface ThreadService {

    void create(
            String topLevelComment, String authorId, String channelId, String authorName, String authorAvatarUrl, String authorUrl, String videoId
    );

    ThreadListResponse listThreads(
            List<String> ids,
            String videoId,
            String channelId,
            int maxResults,
            String order,
            String pageToken
    );

}
