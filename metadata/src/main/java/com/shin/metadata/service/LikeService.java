package com.shin.metadata.service;

import com.shin.metadata.dto.VideoLikesResponse;

import java.util.UUID;

public interface LikeService {

    VideoLikesResponse like(UUID userId, UUID videoId);

    VideoLikesResponse unlike(UUID userId, UUID videoId);

    VideoLikesResponse getLikeInfo(UUID userId, UUID videoId);

}
