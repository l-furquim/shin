package com.shin.metadata.repository;

import com.shin.metadata.model.Video;
import com.shin.metadata.model.enums.VideoVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findByCreatorIdAndVisibility(
        UUID creatorId,
        VideoVisibility visibility,
        Pageable pageable
    );

    Page<Video> findByVideoCategory_NameContainsIgnoreCase(
        String categoryName,
        Pageable pageable
    );

    Page<Video> findAllByOrderByPublishedAtDesc(Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Video v
        SET v.likeCount = GREATEST(0, v.likeCount + :delta)
        WHERE v.id = :videoId
    """)
    int applyLikeDelta(@Param("videoId") UUID videoId, @Param("delta") Long delta);

}
