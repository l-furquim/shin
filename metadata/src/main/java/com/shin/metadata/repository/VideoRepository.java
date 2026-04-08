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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        ORDER BY v.createdAt DESC, v.id DESC
    """)
    List<Video> findAllPublicWithoutCursorDesc(Pageable pageable);

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND (v.createdAt < :cursorTimestamp
            OR (v.createdAt = :cursorTimestamp AND v.id < :cursorId))
        ORDER BY v.createdAt DESC, v.id DESC
    """)
    List<Video> findAllPublicWithCursorDesc(
        @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        ORDER BY v.createdAt ASC, v.id ASC
    """)
    List<Video> findAllPublicWithoutCursorAsc(Pageable pageable);

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND (v.createdAt > :cursorTimestamp
            OR (v.createdAt = :cursorTimestamp AND v.id > :cursorId))
        ORDER BY v.createdAt ASC, v.id ASC
    """)
    List<Video> findAllPublicWithCursorAsc(
        @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND v.videoCategory.id = :categoryId
        ORDER BY v.createdAt DESC, v.id DESC
    """)
    List<Video> findByCategoryWithoutCursorDesc(
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND v.videoCategory.id = :categoryId
        AND (v.createdAt < :cursorTimestamp
            OR (v.createdAt = :cursorTimestamp AND v.id < :cursorId))
        ORDER BY v.createdAt DESC, v.id DESC
    """)
    List<Video> findByCategoryWithCursorDesc(
        @Param("categoryId") Long categoryId,
        @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND v.videoCategory.id = :categoryId
        ORDER BY v.createdAt ASC, v.id ASC
    """)
    List<Video> findByCategoryWithoutCursorAsc(
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    @Query("""
        SELECT v FROM Video v
        WHERE v.visibility = 'PUBLIC'
        AND v.videoCategory.id = :categoryId
        AND (v.createdAt > :cursorTimestamp
            OR (v.createdAt = :cursorTimestamp AND v.id > :cursorId))
        ORDER BY v.createdAt ASC, v.id ASC
    """)
    List<Video> findByCategoryWithCursorAsc(
        @Param("categoryId") Long categoryId,
        @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
        @Param("cursorId") UUID cursorId,
        Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE Video v
        SET v.viewCount = GREATEST(0, COALESCE(v.viewCount, 0) + :delta)
        WHERE v.id = :videoId
    """)
    int applyViewDelta(@Param("videoId") UUID videoId, @Param("delta") Long delta);

    @Modifying
    @Query("""
        UPDATE Video v
        SET v.thumbnailUrl = :thumbnailUrl
        WHERE v.id = :videoId
    """)
    int updateVideoThumbnail(@Param("videoId") UUID videoId, @Param("thumbnailUrl") String thumbnailUrl);
}
