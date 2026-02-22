package com.shin.interaction.repository;

import com.shin.interaction.model.VideoLike;
import com.shin.interaction.model.VideoLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<VideoLike, VideoLikeId> {
}
