package com.shin.metadata.service.impl;

import com.shin.metadata.dto.CreateVideoRequest;
import com.shin.metadata.dto.CreateVideoResponse;
import com.shin.metadata.dto.GetVideoByIdResponse;
import com.shin.metadata.dto.PatchVideoByIdRequest;
import com.shin.metadata.dto.PatchVideoByIdResponse;
import com.shin.metadata.dto.SearchVideosResponse;
import com.shin.metadata.dto.VideoDto;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.model.Video;
import com.shin.metadata.model.enums.VideoVisibility;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.VideoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;

    @Override
    public CreateVideoResponse createVideo(CreateVideoRequest request) {
        Video video = Video.builder()
            .videoId(request.videoId())
            .title(request.title())
            .description(request.description())
            .visibility(request.visibility())
            .status(request.status())
            .accountId(request.accountId())
            .resolutions(new ArrayList<>(request.resolutions()))
            .build();

        video = videoRepository.save(video);

        return new CreateVideoResponse(
            video.getId(),
            video.getVideoId(),
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
            video.getVideoId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getAccountId() != null ? video.getAccountId() : "",
            video.getOnlyForAdults(),
            video.getVideoKey(),
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
        if (request.videoKey() != null) {
            video.setVideoKey(request.videoKey());
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
        if (request.tags() != null) {
            video.updateTags(request.tags());
        }
        if (request.scheduledPublishAt() != null) {
            video.setScheduledPublishAt(request.scheduledPublishAt());
        }
        if (request.status() != null) {
            video.setStatus(request.status());
        }

        return new PatchVideoByIdResponse(
            video.getId(),
            video.getVideoId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getAccountId() != null ? video.getAccountId() : "",
            video.getOnlyForAdults(),
            video.getVideoKey(),
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

        var videos = videoRepository.findByAccountIdAndVisibility(
            accountId,
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

    private VideoDto toDto(Video video) {
        return new VideoDto(
            video.getId(),
            video.getVideoId(),
            video.getTitle(),
            video.getDescription(),
            video.getVisibility(),
            video.getAccountId() != null ? video.getAccountId() : "",
            video.getOnlyForAdults(),
            video.getVideoKey(),
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
