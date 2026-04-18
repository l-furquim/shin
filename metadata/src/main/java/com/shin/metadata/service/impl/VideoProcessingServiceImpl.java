package com.shin.metadata.service.impl;

import com.shin.metadata.dto.VideoProgressResponse;
import com.shin.metadata.exception.ForbiddenVideoOperationException;
import com.shin.metadata.exception.InvalidVideoProcessingException;
import com.shin.metadata.exception.VideoProcessingNotFound;
import com.shin.metadata.model.VideoProcessing;
import com.shin.metadata.model.enums.UploadingStatus;
import com.shin.metadata.repository.VideoProcessingRepository;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class VideoProcessingServiceImpl implements VideoProcessingService {

    private final VideoProcessingRepository videoProcessingRepository;
    private final VideoRepository videoRepository;

    @Override
    public VideoProcessing create(UUID videoId, String fileName, Long durationSeconds, Long fileSizeBytes, String fileType) {
        final var videoProcessing = VideoProcessing.builder()
                .videoId(videoId)
                .fileName(fileName)
                .durationSeconds(durationSeconds)
                .fileSizeBytes(fileSizeBytes)
                .fileType(fileType)
                .transcodingProgress(0)
                .uploadingProgress(0)
                .uploadingStatus(UploadingStatus.WAITING)
                .startedAt(java.time.LocalDateTime.now())
                .build();

        return videoProcessingRepository.save(videoProcessing);
    }

    @Override
    public VideoProcessing update(
            UUID videoId,
            String fileName,
            Long durationSeconds,
            Long fileSizeBytes,
            String fileType,
            Integer transcodingProgress,
            Integer uploadingProgress,
            String failureReason
    ) {
        final var videoProcessing = videoProcessingRepository
                .findById(videoId)
                .orElseThrow(VideoProcessingNotFound::new);

        if (fileName != null && !fileName.isBlank()) {
           videoProcessing.setFileName(fileName);
        }

        if (durationSeconds != null && durationSeconds > 0) {
            videoProcessing.setDurationSeconds(durationSeconds);
        }

        if (fileSizeBytes != null && fileSizeBytes > 0) {
            videoProcessing.setFileSizeBytes(fileSizeBytes);
        }

        if (fileType != null && !fileType.isBlank()) {
            videoProcessing.setFileType(fileType);
        }

        if (transcodingProgress != null && transcodingProgress >= 0) {
            videoProcessing.setTranscodingProgress(transcodingProgress);
        }

        if (uploadingProgress != null && uploadingProgress >= 0) {
            videoProcessing.setUploadingProgress(uploadingProgress);
        }

        if (failureReason != null && !failureReason.isBlank()) {
            videoProcessing.setFailureReason(failureReason);
        }

        return videoProcessingRepository.save(videoProcessing);
    }

    @Override
    public VideoProgressResponse progress(UUID userId, UUID videoId) {
        final var videoProcessing = videoProcessingRepository
                .findById(videoId)
                .orElseThrow(VideoProcessingNotFound::new);

        final var videoRelated = this.videoRepository.findById(videoId).orElseThrow(VideoProcessingNotFound::new);

        if (!videoRelated.getCreatorId().equals(userId)) {
            throw new ForbiddenVideoOperationException();
        }

        return new VideoProgressResponse(
                videoProcessing.getTranscodingProgress(),
                videoProcessing.getUploadingProgress(),
                videoProcessing.getFailureReason(),
                videoProcessing.getTranscodingStatus().toString().toLowerCase(),
                videoProcessing.getUploadingStatus().toString().toLowerCase(),
                videoProcessing.getFileSizeBytes(),
                videoProcessing.getStartedAt()
        );
    }

    @Override
    public VideoProcessing findById(UUID videoId) {
        return videoProcessingRepository
                .findById(videoId)
                .orElseThrow(VideoProcessingNotFound::new);
    }
}
