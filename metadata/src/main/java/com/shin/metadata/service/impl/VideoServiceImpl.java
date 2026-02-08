package com.shin.metadata.service.impl;

import com.shin.metadata.dto.*;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.model.Tag;
import com.shin.metadata.model.Video;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.TagService;
import com.shin.metadata.service.VideoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final TagService tagService;

    @Override
    public InitVideoResponse initVideo(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidVideoRequestException("User ID must not be empty");
        }

        UUID videoId = UUID.randomUUID();

        Video video = Video.builder()
            .id(videoId)
            .creatorId(UUID.fromString(userId))
            .status(ProcessingStatus.DRAFT)
            .visibility(VideoVisibility.PRIVATE)
            .onlyForAdults(false)
            .build();

        video = videoRepository.save(video);

        return new InitVideoResponse(
            video.getId(),
            video.getStatus(),
            video.getCreatedAt().plusHours(24) // Expires in one day if not published
        );
    }

    @Override
    public CreateVideoResponse createVideo(CreateVideoRequest request) {
        Video video = Video.builder()
             .id(request.videoId() != null ? request.videoId() : UUID.randomUUID())
            .title(request.title())
            .description(request.description())
            .visibility(request.visibility())
            .status(request.status())
            .creatorId(UUID.fromString(request.accountId()))
            .resolutions(request.resolutions())
            .build();

        video = videoRepository.save(video);

        return new CreateVideoResponse(
            video.getId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getStatus(),
            video.getResolutions()
        );
    }

    @Override
    public GetVideoByIdResponse getVideoById(UUID id) {
        Video video = videoRepository.findById(id)
            .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        return new GetVideoByIdResponse(
            video.getId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getCreatorId().toString(),
            video.getOnlyForAdults(),
            video.getUploadKey(),
            video.getThumbnailUrl(),
            video.getVideoCategory(),
            video.getDefaultLanguage(),
            video.getPublishedLocale(),
            video.getTags(),
            video.getDuration() != null ? video.getDuration() : 0L,
            video.getResolutions(),
            video.getPublishedAt(),
            video.getScheduledPublishAt(),
            video.getCreatedAt(),
            video.getUpdatedAt(),
            video.getStatus()
        );
    }

    @Override
    @Transactional
    public PatchVideoByIdResponse patchVideoById(UUID id, PatchVideoByIdRequest request) {
        Video video = videoRepository.findById(id)
            .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        if (request.title() != null) {
            video.setTitle(request.title());
        }
        if (request.description() != null) {
            video.setDescription(request.description());
        }
        if (request.duration() != null) {
            video.setDuration(request.duration());
        }
        if (request.resolutions() != null) {
            video.updateResolutions(request.resolutions());
        }
        if (request.uploadKey() != null) {
            video.setUploadKey(request.uploadKey());
        }
        if (request.thumbnailUrl() != null) {
            video.setThumbnailUrl(request.thumbnailUrl());
        }
        if (request.videoCategory() != null) {
            video.setVideoCategory(request.videoCategory());
        }
        if (request.visibility() != null) {
            video.setVisibility(request.visibility());
        }
        if (request.defaultLanguage() != null) {
            video.setDefaultLanguage(request.defaultLanguage());
        }
        if (request.onlyForAdults() != null) {
            video.setOnlyForAdults(request.onlyForAdults());
        }

        if (request.tagsToAdd() != null && !request.tagsToAdd().isEmpty()) {
            Set<Tag> tagsToAdd = tagService.findOrCreateTags(request.tagsToAdd());
            for (Tag tag : tagsToAdd) {
                if (!video.getTags().contains(tag)) {
                    video.addTag(tag);
                    log.debug("Added tag {} to video {}", tag.getName(), video.getId());
                } else {
                    log.debug("Tag {} already on video {}, skipping", tag.getName(), video.getId());
                }
            }
        }

        if (request.tagsToRemove() != null && !request.tagsToRemove().isEmpty()) {
            Set<Tag> tagsToRemove = tagService.findTagsOrThrow(request.tagsToRemove());
            for (Tag tag : tagsToRemove) {
                if (video.getTags().contains(tag)) {
                    video.removeTag(tag);
                    log.debug("Removed tag {} from video {}", tag.getName(), video.getId());
                } else {
                    log.warn("Tag {} not found on video {}, skipping removal", tag.getName(), video.getId());
                }
            }
        }

        if (request.scheduledPublishAt() != null) {
            video.setScheduledPublishAt(request.scheduledPublishAt());
        }
        if (request.status() != null) {
            video.setStatus(request.status());
        }

        return new PatchVideoByIdResponse(
            video.getId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getCreatorId().toString(),
            video.getOnlyForAdults(),
            video.getUploadKey(),
            video.getThumbnailUrl(),
            video.getVideoCategory(),
            video.getDefaultLanguage(),
            video.getPublishedLocale(),
            video.getTags(),
            video.getDuration() != null ? video.getDuration() : 0.0,
            video.getResolutions(),
            video.getPublishedAt(),
            video.getScheduledPublishAt(),
            video.getCreatedAt(),
            video.getUpdatedAt(),
            video.getStatus()
        );
    }

    @Override
    @Transactional
    public void deleteVideoById(UUID id) {
        Video video = videoRepository.findById(id)
            .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        videoRepository.delete(video);
    }

    @Override
    public SearchVideosResponse searchByAccountId(String accountId, int start, int end) {
        if (accountId == null || accountId.isBlank()) {
            throw new InvalidVideoRequestException("Account ID must not be empty");
        }

        var videos = videoRepository.findByCreatorIdAndVisibility(
            UUID.fromString(accountId),
            VideoVisibility.PUBLIC,
            PageRequest.of(start, end)
        );

        return new SearchVideosResponse(
            videos.getContent().stream()
                .map(this::toDto)
                .toList()
        );
    }

    @Override
    public SearchVideosResponse searchByCategory(String categoryName, int start, int end) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new InvalidVideoRequestException("Category must not be empty");
        }

        var videos = videoRepository.findByVideoCategory_NameContainsIgnoreCase(
            categoryName,
            PageRequest.of(start, end)
        );

        return new SearchVideosResponse(
            videos.getContent().stream()
                .map(this::toDto)
                .toList()
        );
    }

    @Override
    public SearchVideosResponse searchAll(int start, int end) {
        var videos = videoRepository.findAllByOrderByPublishedAtDesc(
            PageRequest.of(start, end)
        );

        return new SearchVideosResponse(
            videos.getContent().stream()
                .map(this::toDto)
                .toList()
        );
    }

    @Override
    public void updateVideoProcessingStatus(String videoId, String status, String[] resolutions, Double duration) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + videoId + " not found"));

        final var isCompleted = status.equals("completed");

        video.setStatus(
                isCompleted ? ProcessingStatus.PROCESSED : ProcessingStatus.FAILED
        );

        if(isCompleted) {
            log.info("Video failed to be encoded {}", videoId);
            return;
        }

        video.setResolutions(String.join(",", resolutions));
        video.setDuration(duration);

        videoRepository.save(video);
    }

    private VideoDto toDto(Video video) {
        return new VideoDto(
            video.getId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getCreatorId().toString(),
            video.getOnlyForAdults(),
            video.getUploadKey(),
            video.getThumbnailUrl(),
            video.getVideoCategory(),
            video.getDefaultLanguage(),
            video.getPublishedLocale(),
            video.getTags(),
            video.getDuration() != null ? video.getDuration() : 0L,
            video.getResolutions(),
            video.getPublishedAt(),
            video.getScheduledPublishAt(),
            video.getCreatedAt(),
            video.getUpdatedAt(),
            video.getStatus()
        );
    }
}
