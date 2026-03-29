package com.shin.metadata.service.impl;

import com.shin.commons.models.PageInfo;
import com.shin.metadata.client.UserServiceClient;
import com.shin.metadata.dto.*;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.model.Tag;
import com.shin.metadata.model.ThumbnailProfile;
import com.shin.metadata.model.Video;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.LikeService;
import com.shin.metadata.service.ProcessingProgressService;
import com.shin.metadata.service.TagService;
import com.shin.metadata.service.VideoService;
import com.shin.metadata.service.ViewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private static final String CURSOR_NEXT = "N";
    private static final String CURSOR_PREV = "P";
    private static final List<ThumbnailProfile> THUMBNAIL_PROFILES = List.of(
            new ThumbnailProfile("default", 120, 90),
            new ThumbnailProfile("medium", 320, 180),
            new ThumbnailProfile("high", 480, 360),
            new ThumbnailProfile("standard", 640, 480),
            new ThumbnailProfile("maxres", 1280, 720)
    );

    private final VideoRepository videoRepository;
    private final TagService tagService;
    private final LikeService likeService;
    private final ViewService viewService;
    private final UserServiceClient userServiceClient;
    private final ProcessingProgressService processingProgressService;
    @Value("${media.thumbnail-base-url:}")
    private String thumbnailBaseUrl;

    @Override
    public InitVideoResponse initVideo(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidVideoRequestException("User ID must not be empty");
        }

        Video video = Video.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.fromString(userId))
                .status(ProcessingStatus.DRAFT)
                .visibility(VideoVisibility.PRIVATE)
                .onlyForAdults(false)
                .likeCount(0L)
                .build();

        video = videoRepository.save(video);

        return new InitVideoResponse(
                video.getId(),
                video.getStatus(),
                video.getCreatedAt().plusHours(24)
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
                .likeCount(0L)
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
    public VideoDto getVideoById(UUID id, UUID userId, Set<VideoField> fields) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        Boolean likedByMe = null;
        if (userId != null) {
            try {
                likedByMe = likeService.getLikeInfo(userId, id).likedByMe();
            } catch (Exception e) {
                log.warn("Could not resolve like status for user {} / video {}: {}", userId, id, e.getMessage());
                likedByMe = false;
            }
        }

        Long effectiveViewCount = fields.contains(VideoField.STATISTICS)
                ? viewService.getEffectiveVideoViews(video.getId(), video.getViewCount())
                : 0L;
        return toDto(video, likedByMe, fields, isOwner(video, userId), effectiveViewCount);
    }

    @Override
    @Transactional
    public VideoDto patchVideoById(UUID id, PatchVideoByIdRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        if (request.title() != null) video.setTitle(request.title());
        if (request.description() != null) video.setDescription(request.description());
        if (request.duration() != null) video.setDuration(request.duration());
        if (request.resolutions() != null) video.updateResolutions(request.resolutions());
        if (request.uploadKey() != null) video.setUploadKey(request.uploadKey());
        if (request.thumbnailUrl() != null) video.setThumbnailUrl(request.thumbnailUrl());
        if (request.videoCategory() != null) video.setVideoCategory(request.videoCategory());
        if (request.visibility() != null) video.setVisibility(request.visibility());
        if (request.defaultLanguage() != null) video.setDefaultLanguage(request.defaultLanguage());
        if (request.onlyForAdults() != null) video.setOnlyForAdults(request.onlyForAdults());
        if (request.scheduledPublishAt() != null) video.setScheduledPublishAt(request.scheduledPublishAt());
        if (request.status() != null) video.setStatus(request.status());

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

        Long effectiveViewCount = viewService.getEffectiveVideoViews(video.getId(), video.getViewCount());
        return toDto(video, null, EnumSet.allOf(VideoField.class), true, effectiveViewCount);
    }

    @Override
    @Transactional
    public void deleteVideoById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));
        videoRepository.delete(video);
    }

    @Override
    public SearchVideosResponse search(SearchVideosRequest request, UUID userId, Set<VideoField> fields) {
        if (request.id() != null && !request.id().isBlank()) {
            Video video = videoRepository.findById(UUID.fromString(request.id()))
                    .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + request.id() + " not found"));
            Long effectiveViewCount = fields.contains(VideoField.STATISTICS)
                    ? viewService.getEffectiveVideoViews(video.getId(), video.getViewCount())
                    : 0L;
            return new SearchVideosResponse(
                    null, null,
                    new PageInfo(1L, (long) request.limit()),
                    List.of(toDto(video, null, fields, isOwner(video, userId), effectiveViewCount))
            );
        }

        LocalDateTime cursorTimestamp = null;
        UUID cursorId = null;
        String cursorDirection = CURSOR_NEXT;

        if (request.cursor() != null && !request.cursor().isBlank()) {
            String[] parts = decodeCursor(request.cursor());
            cursorTimestamp = LocalDateTime.parse(parts[0]);
            cursorId = UUID.fromString(parts[1]);
            cursorDirection = parts[2];
        }

        int fetchSize = request.limit() + 1;
        Pageable pageable = Pageable.ofSize(fetchSize);

        Long categoryId = request.categoryId() != null && !request.categoryId().isBlank()
                ? Long.parseLong(request.categoryId()) : null;

        boolean isForward = CURSOR_NEXT.equals(cursorDirection);
        List<Video> videos = fetchVideos(isForward, categoryId, cursorTimestamp, cursorId, pageable);

        boolean hasMore = videos.size() > request.limit();
        if (hasMore) {
            videos = new ArrayList<>(videos.subList(0, request.limit()));
        }

        if (!isForward) {
            Collections.reverse(videos);
        }

        String nextPageToken = null;
        String prevPageToken = null;

        if (!videos.isEmpty()) {
            Video first = videos.getFirst();
            Video last = videos.getLast();

            if (isForward) {
                if (hasMore) {
                    nextPageToken = encodeCursor(last.getCreatedAt(), last.getId(), CURSOR_NEXT);
                }
                if (cursorTimestamp != null) {
                    prevPageToken = encodeCursor(first.getCreatedAt(), first.getId(), CURSOR_PREV);
                }
            } else {
                nextPageToken = encodeCursor(first.getCreatedAt(), first.getId(), CURSOR_NEXT);
                if (hasMore) {
                    prevPageToken = encodeCursor(last.getCreatedAt(), last.getId(), CURSOR_PREV);
                }
            }
        }

        Map<UUID, Long> effectiveViewsByVideoId = Collections.emptyMap();
        if (fields.contains(VideoField.STATISTICS) && !videos.isEmpty()) {
            Map<UUID, Long> persistedViewsByVideoId = videos.stream()
                    .collect(Collectors.toMap(Video::getId, v -> v.getViewCount() == null ? 0L : v.getViewCount()));
            effectiveViewsByVideoId = viewService.getEffectiveVideoViews(persistedViewsByVideoId);
        }

        Map<UUID, Long> finalEffectiveViewsByVideoId = effectiveViewsByVideoId;
        List<VideoDto> items = videos.stream()
                .map(v -> toDto(
                        v,
                        null,
                        fields,
                        isOwner(v, userId),
                        finalEffectiveViewsByVideoId.getOrDefault(v.getId(), v.getViewCount() == null ? 0L : v.getViewCount())
                ))
                .toList();

        return new SearchVideosResponse(
                nextPageToken,
                prevPageToken,
                new PageInfo((long) items.size(), (long) request.limit()),
                items
        );
    }

    @Override
    public void increaseVideoView(UUID videoId, UUID userId) {
        if (videoId == null || userId == null) {
            throw new InvalidVideoRequestException("Video ID and user ID must not be null");
        }
        viewService.increaseView(videoId, userId.toString());
    }

    @Override
    public void increaseVideoView(UUID videoId, String viewerKey) {
        if (videoId == null || viewerKey == null || viewerKey.isBlank()) {
            throw new InvalidVideoRequestException("Video ID and viewer key must not be null");
        }
        viewService.increaseView(videoId, viewerKey);
    }

    private List<Video> fetchVideos(boolean isForward, Long categoryId,
                                    LocalDateTime cursorTimestamp, UUID cursorId, Pageable pageable) {
        boolean hasCursor = cursorTimestamp != null && cursorId != null;

        if (isForward) {
            if (categoryId != null) {
                return hasCursor
                        ? videoRepository.findByCategoryWithCursorDesc(categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByCategoryWithoutCursorDesc(categoryId, pageable);
            }
            return hasCursor
                    ? videoRepository.findAllPublicWithCursorDesc(cursorTimestamp, cursorId, pageable)
                    : videoRepository.findAllPublicWithoutCursorDesc(pageable);
        } else {
            if (categoryId != null) {
                return hasCursor
                        ? videoRepository.findByCategoryWithCursorAsc(categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByCategoryWithoutCursorAsc(categoryId, pageable);
            }
            return hasCursor
                    ? videoRepository.findAllPublicWithCursorAsc(cursorTimestamp, cursorId, pageable)
                    : videoRepository.findAllPublicWithoutCursorAsc(pageable);
        }
    }

    @Override
    public void updateVideoProcessingStatus(String videoId, String status, String processedPath, String[] resolutions, Double duration, String fileName, Long fileSize, String fileType) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + videoId + " not found"));

        final var isCompleted = status.equals("completed");

        video.setStatus(
                isCompleted ? ProcessingStatus.PROCESSED : ProcessingStatus.FAILED
        );

        if(!isCompleted) {
            log.info("Video failed to be encoded {}", videoId);
        }

        if (resolutions != null) {
            video.setResolutions(String.join(",", resolutions));
        }
        if (processedPath != null && !processedPath.isBlank()) {
            video.setUploadKey(processedPath);
        }
        video.setDuration(duration);
        video.setFileName(fileName);
        video.setFileSize(fileSize);
        video.setFileType(fileType);

        videoRepository.save(video);
    }

    @Override
    @Transactional
    public void updateVideoThumbnail(String videoId, String thumbnailUrl) {
        UUID id = UUID.fromString(videoId);
        String resolvedUrl = resolveThumbnailUrl(thumbnailUrl);
        int updated = videoRepository.updateVideoThumbnail(id, resolvedUrl);
        if (updated <= 0) {
            throw new InvalidVideoRequestException("Video with ID " + videoId + " not found");
        }
    }

    private String resolveThumbnailUrl(String thumbnailValue) {
        if (thumbnailValue == null || thumbnailValue.isBlank()) {
            return thumbnailValue;
        }

        String normalizedValue = thumbnailValue.trim();
        if (normalizedValue.startsWith("http://") || normalizedValue.startsWith("https://")) {
            return normalizedValue;
        }

        if (thumbnailBaseUrl == null || thumbnailBaseUrl.isBlank()) {
            return normalizedValue;
        }
        if (thumbnailBaseUrl.contains("${")) {
            return normalizedValue;
        }

        String normalizedBase = thumbnailBaseUrl.trim();
        while (normalizedBase.startsWith("https://https://") || normalizedBase.startsWith("http://http://")) {
            normalizedBase = normalizedBase.replaceFirst("^https?://", "");
        }

        normalizedBase = normalizedBase.endsWith("/")
                ? normalizedBase.substring(0, normalizedBase.length() - 1)
                : normalizedBase;

        if (!normalizedBase.startsWith("http://") && !normalizedBase.startsWith("https://")) {
            normalizedBase = "https://" + normalizedBase;
        }

        String normalizedPath = normalizedValue.startsWith("/") ? normalizedValue.substring(1) : normalizedValue;
        if (!normalizedPath.startsWith("thumbnails/")) {
            normalizedPath = "thumbnails/" + normalizedPath;
        }

        return normalizedBase + "/" + normalizedPath;
    }

    private VideoDto toDto(Video video, Boolean likedByMe, Set<VideoField> fields, boolean isOwner, Long effectiveViewCount) {
        Map<String, Thumbnail> thumbnails = fields.contains(VideoField.THUMBNAILS) && video.getThumbnailUrl() != null
                ? buildThumbnailMap(video.getThumbnailUrl())
                : null;

        ContentDetails contentDetails = fields.contains(VideoField.CONTENT_DETAILS)
                ? new ContentDetails(
                video.getResolutions(),
                video.getDuration() != null ? video.getDuration() : 0.0,
                video.getUploadKey(),
                video.getDefaultLanguage(),
                video.getPublishedLocale(),
                video.getOnlyForAdults())
                : null;

        Statistics statistics = fields.contains(VideoField.STATISTICS)
                ? new Statistics(
                video.getLikeCount(),
                effectiveViewCount,
                null)
                : null;

        FileDetails fileDetails = (fields.contains(VideoField.FILE_DETAILS) && isOwner)
                ? new FileDetails(video.getFileName(), video.getFileSize(), video.getFileType())
                : null;

        ProcessingDetails processingDetails = (fields.contains(VideoField.PROCESSING_DETAILS) && isOwner)
                ? new ProcessingDetails(
                video.getStatus(),
                null,
                processingProgressService.getProgress(video.getId()))
                : null;

        Channel channel = fields.contains(VideoField.CHANNEL)
                ? resolveChannel(video.getCreatorId())
                : null;

        Set<String> tagNames = fields.contains(VideoField.TAGS) && video.getTags() != null
                ? video.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : null;

        String categoryId = video.getVideoCategory() != null
                ? video.getVideoCategory().getId().toString()
                : null;

        return new VideoDto(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getVisibility(),
                categoryId,
                thumbnails,
                contentDetails,
                statistics,
                likedByMe,
                fileDetails,
                processingDetails,
                channel,
                tagNames,
                video.getPublishedAt(),
                video.getScheduledPublishAt(),
                video.getCreatedAt(),
                video.getUpdatedAt()
        );
    }

    private Channel resolveChannel(UUID creatorId) {
        if (creatorId == null) {
            return new Channel(null, null, null);
        }
        try {
            CreatorResponse creator = userServiceClient.getCreatorById(creatorId);
            return new Channel(creator.id().toString(), creator.displayName(), creator.avatar());
        } catch (Exception e) {
            log.warn("Could not resolve channel info for creatorId {}: {}", creatorId, e.getMessage());
            return new Channel(creatorId.toString(), null, null);
        }
    }

    private boolean isOwner(Video video, UUID userId) {
        return userId != null && userId.equals(video.getCreatorId());
    }

    private Map<String, Thumbnail> buildThumbnailMap(String baseThumbnailUrl) {
        String normalizedBase = baseThumbnailUrl.endsWith("/")
                ? baseThumbnailUrl.substring(0, baseThumbnailUrl.length() - 1)
                : baseThumbnailUrl;

        Map<String, Thumbnail> thumbnails = new LinkedHashMap<>();
        for (ThumbnailProfile profile : THUMBNAIL_PROFILES) {
            thumbnails.put(
                    profile.profile(),
                    new Thumbnail(
                            normalizedBase + "/" + profile.profile() + ".jpg",
                            profile.width(),
                            profile.height()
                    )
            );
        }

        return thumbnails;
    }

    private String encodeCursor(LocalDateTime timestamp, UUID id, String direction) {
        String raw = timestamp + "|" + id + "|" + direction;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String[] decodeCursor(String cursor) {
        return new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", 3);
    }
}
