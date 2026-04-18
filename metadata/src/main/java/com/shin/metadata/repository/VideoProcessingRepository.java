package com.shin.metadata.repository;

import com.shin.metadata.model.VideoProcessing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoProcessingRepository extends JpaRepository<VideoProcessing, UUID> {
}
