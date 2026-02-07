package com.shin.metadata.repository;

import com.shin.metadata.model.Video;
import com.shin.metadata.model.enums.VideoVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findByAccountIdAndVisibility(
        String accountId,
        VideoVisibility visibility,
        Pageable pageable
    );

    Page<Video> findByVideoCategory_NameContainsIgnoreCase(
        String categoryName,
        Pageable pageable
    );

    Page<Video> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
