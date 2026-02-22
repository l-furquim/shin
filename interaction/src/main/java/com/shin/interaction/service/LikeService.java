package com.shin.interaction.service;

import com.shin.interaction.dto.DeleteLikeResponse;
import com.shin.interaction.dto.GetVideoLikesResponse;

import java.util.UUID;

public interface LikeService {

    void create(
            UUID userId,
            UUID videoId
    );

    GetVideoLikesResponse get(
            UUID userId,
            UUID videoId
    );

    DeleteLikeResponse delete(
            UUID userId,
            UUID videoId
    );


}
