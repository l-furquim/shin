package com.shin.metadata.repository;

import com.shin.metadata.model.VideoLike;
import com.shin.metadata.model.VideoLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<VideoLike, VideoLikeId> {
}
