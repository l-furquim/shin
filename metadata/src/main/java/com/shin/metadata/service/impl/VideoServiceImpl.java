package com.shin.metadata.service.impl;

import com.shin.commons.models.PageInfo;
import com.shin.metadata.client.InteractionServiceClient;
import com.shin.metadata.client.UserServiceClient;
import com.shin.metadata.dto.*;
import com.shin.metadata.exception.*;
import com.shin.metadata.model.Tag;
import com.shin.metadata.model.ThumbnailProfile;
import com.shin.metadata.model.Video;
import com.shin.metadata.model.VideoProcessing;
import com.shin.metadata.model.enums.VideoVisibility;
import com.shin.metadata.producer.VideoPublishedProducer;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.shin.commons.util.PageTokenUtil;
import org.springframework.transaction.annotation.Transactional;

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
    private final VideoCategoryService videoCategoryService;
    private final VideoProcessingService videoProcessingService;
    private final ViewService viewService;
    private final UserServiceClient userServiceClient;
    private final InteractionServiceClient interactionServiceClient;

    private final VideoPublishedProducer videoPublishedProducer;

    @Value("${media.thumbnail-base-url:}")
    private String thumbnailBaseUrl;

    @Override
    public CreateVideoResponse createVideo(CreateVideoRequest request) {
        Video video = Video.builder()
                .id(request.videoId() != null ? request.videoId() : UUID.randomUUID())
                .title(request.title())
                .description(request.description())
                .visibility(request.visibility())
                .likeCount(0L)
                .creatorId(UUID.fromString(request.accountId()))
                .resolutions(request.resolutions())
                .build();

        video = videoRepository.save(video);

        this.videoProcessingService.create(
                video.getId(),
                request.fileName(),
                null,
                request.fileSize(),
                request.fileType()
        );

        return new CreateVideoResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getVisibility(),
                video.getResolutions()
        );
    }

    @Override
    public VideoDto getVideoById(UUID id, UUID userId, Set<VideoField> fields) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

       final var videoProcessing = this.videoProcessingService.findById(video.getId());

        Boolean likedByMe = resolveLikedByMeBatch(List.of(id), userId).get(id.toString());

        Long effectiveViewCount = fields.contains(VideoField.STATISTICS)
                ? viewService.getEffectiveVideoViews(video.getId(), video.getViewCount())
                : 0L;
        return toDto(video, videoProcessing, likedByMe, fields, isOwner(video, userId), effectiveViewCount);
    }

    @Override
    public WatchVideoResponse getWatchVideoById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video with ID " + id + " not found"));

        final var videoProcessing = this.videoProcessingService.findById(video.getId());

        long duration = videoProcessing.getDurationSeconds() == null ? 0L : videoProcessing.getDurationSeconds();

        return new WatchVideoResponse(
                video.getId(),
                video.getCreatorId(),
                video.getTitle(),
                duration,
                video.getDescription(),
                video.getVisibility(),
                videoProcessing.getTranscodingStatus()
        );
    }

    @Override
    public void publish(UUID id, UUID userId) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException("Video with ID " + id + " not found"));

        final var videoProcessing = this.videoProcessingService.findById(video.getId());

        if (!video.getCreatorId().equals(userId)) {
            throw new ForbiddenVideoOperationException();
        }

        if (!videoProcessing.isProcessed()) {
            throw new VideoProcessingException();
        }

        if (video.getVisibility().equals(VideoVisibility.PUBLIC)) {
            throw new VideoAlreadyPublishedException();
        }

        video.setPublishedAt(LocalDateTime.now());
        video.setVisibility(VideoVisibility.PUBLIC);

        try {
            final var event = VideoPublishedEvent.builder()
                    .id(video.getId())
                    .title(video.getTitle())
                    .description(video.getDescription())
                    .language(video.getDefaultLanguage() != null ? video.getDefaultLanguage().getDisplayName() : null)
                    .tags(video.getTags() != null ? video.getTags().stream().map(Tag::getName).collect(Collectors.toSet()) : Set.of())
                    .forAdults(video.getOnlyForAdults() != null && video.getOnlyForAdults())
                    .publishedAt(video.getPublishedAt())
                    .videoLink("/videos/" + video.getId())
                    .duration(Double.valueOf(videoProcessing.getDurationSeconds()))
                    .thumbnailUrl(video.getThumbnailUrl())
                    .categoryName(video.getVideoCategory() != null ? video.getVideoCategory().getName() : null)
                    .visibility(video.getVisibility() != null ? video.getVisibility().name() : null)
                    .build();

            // Send video published event to notification and search service
            this.videoPublishedProducer.sendEvent(event);
        } catch (Exception e) {
            log.error("Error sending video published notification", e);
        }

        this.videoRepository.save(video);
    }

    @Override
    @Transactional
    public VideoDto patchVideoById(UUID id, PatchVideoByIdRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + id + " not found"));

        final var videoProcessing = this.videoProcessingService.findById(video.getId());

        if (request.title() != null) video.setTitle(request.title());
        if (request.description() != null) video.setDescription(request.description());
        if (request.duration() != null) videoProcessing.setDurationSeconds(request.duration());
        if (request.resolutions() != null) video.updateResolutions(request.resolutions());
        if (request.uploadKey() != null) videoProcessing.setUploadKey(request.uploadKey());
        if (request.thumbnailUrl() != null) video.setThumbnailUrl(request.thumbnailUrl());
        if (request.categoryId() != null) video.setVideoCategory(videoCategoryService.findCategoryOrThrow(request.categoryId()));
        if (request.defaultLanguage() != null) video.setDefaultLanguage(request.defaultLanguage());
        if (request.onlyForAdults() != null) video.setOnlyForAdults(request.onlyForAdults());
        if (request.scheduledPublishAt() != null) video.setScheduledPublishAt(request.scheduledPublishAt());

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
        return toDto(video, videoProcessing, null, EnumSet.allOf(VideoField.class), true, effectiveViewCount);
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
        if (request.ids() != null && !request.ids().isBlank()) {
            return searchByIds(request, userId, fields);
        }

        if (request.id() != null && !request.id().isBlank()) {
            Video video = videoRepository.findById(UUID.fromString(request.id()))
                    .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + request.id() + " not found"));

            final var videoProcessing = this.videoProcessingService.findById(video.getId());

            Boolean likedByMe = null;

            final var likedByMeRequest = request.myRating() != null && !request.myRating().isBlank() && MyRating.fromKey(request.myRating()).isPresent();

            if (likedByMeRequest) {
                likedByMe = resolveLikedByMeBatch(List.of(video.getId()), userId).get(video.getId().toString());
            }

            Long effectiveViewCount = fields.contains(VideoField.STATISTICS)
                    ? viewService.getEffectiveVideoViews(video.getId(), video.getViewCount())
                    : 0L;
            return new SearchVideosResponse(
                    null, null,
                    new PageInfo(1L, (long) request.limit()),
                    List.of(toDto(video, videoProcessing, likedByMe,fields, isOwner(video, userId), effectiveViewCount))
            );
        }

        LocalDateTime cursorTimestamp = null;
        UUID cursorId = null;
        String cursorDirection = CURSOR_NEXT;

        if (request.cursor() != null && !request.cursor().isBlank()) {
            Map<String, String> parts = PageTokenUtil.decode(request.cursor());
            cursorTimestamp = LocalDateTime.parse(parts.get("createdAt"));
            cursorId = UUID.fromString(parts.get("id"));
            cursorDirection = parts.get("direction");
        }

        int fetchSize = request.limit() + 1;
        Pageable pageable = Pageable.ofSize(fetchSize);

        Long categoryId = request.categoryId() != null && !request.categoryId().isBlank()
                ? Long.parseLong(request.categoryId()) : null;

        boolean isForward = CURSOR_NEXT.equals(cursorDirection);
        UUID ownerId = request.forMine() && userId != null ? userId : null;
        List<Video> videos = fetchVideos(isForward, request.channelId(), categoryId, cursorTimestamp, cursorId, pageable, ownerId);

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

        List<UUID> videoIds = videos.stream().map(Video::getId).toList();
        Map<String, Boolean> likedByMeByVideoId = resolveLikedByMeBatch(videoIds, userId);

        Map<UUID, Long> finalEffectiveViewsByVideoId = effectiveViewsByVideoId;
        List<VideoDto> items = videos.stream()
                .map(v -> toDto(
                        v,
                        this.videoProcessingService.findById(v.getId()),
                        likedByMeByVideoId.get(v.getId().toString()),
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

    private SearchVideosResponse searchByIds(SearchVideosRequest request, UUID userId, Set<VideoField> fields) {
        List<UUID> ids = Arrays.stream(request.ids().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .toList();

        if (ids.isEmpty()) {
            return new SearchVideosResponse(
                    null,
                    null,
                    new PageInfo(0L, (long) request.limit()),
                    List.of()
            );
        }

        LocalDateTime cursorTimestamp = null;
        UUID cursorId = null;
        String cursorDirection = CURSOR_NEXT;

        if (request.cursor() != null && !request.cursor().isBlank()) {
            Map<String, String> parts = PageTokenUtil.decode(request.cursor());
            cursorTimestamp = LocalDateTime.parse(parts.get("createdAt"));
            cursorId = UUID.fromString(parts.get("id"));
            cursorDirection = parts.get("direction");
        }

        int fetchSize = request.limit() + 1;
        Pageable pageable = Pageable.ofSize(fetchSize);

        Long categoryId = request.categoryId() != null && !request.categoryId().isBlank()
                ? Long.parseLong(request.categoryId()) : null;

        boolean isForward = CURSOR_NEXT.equals(cursorDirection);
        List<Video> videos = fetchVideosByIds(ids, isForward, categoryId, cursorTimestamp, cursorId, pageable);

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

        List<UUID> videoIds = videos.stream().map(Video::getId).toList();
        Map<String, Boolean> likedByMeByVideoId = resolveLikedByMeBatch(videoIds, userId);

        Map<UUID, Long> finalEffectiveViewsByVideoId = effectiveViewsByVideoId;
        List<VideoDto> items = videos.stream()
                .map(v -> toDto(
                        v,
                        this.videoProcessingService.findById(v.getId()),
                        likedByMeByVideoId.get(v.getId().toString()),
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
    public void increaseVideoView(UUID videoId, String viewerKey) {
        if (videoId == null || viewerKey == null || viewerKey.isBlank()) {
            throw new InvalidVideoRequestException("Video ID and viewer key must not be null");
        }
        viewService.increaseView(videoId, viewerKey);
    }

    private List<Video> fetchVideos(boolean isForward, UUID channelId, Long categoryId,
                                    LocalDateTime cursorTimestamp, UUID cursorId, Pageable pageable,
                                    UUID ownerId) {
        boolean hasCursor = cursorTimestamp != null && cursorId != null;

        if (ownerId != null) {
            return isForward
                    ? (hasCursor
                        ? videoRepository.findByOwnerWithCursorDesc(ownerId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByOwnerWithoutCursorDesc(ownerId, pageable))
                    : (hasCursor
                        ? videoRepository.findByOwnerWithCursorAsc(ownerId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByOwnerWithoutCursorAsc(ownerId, pageable));
        }

        if (isForward) {
            if (channelId != null && categoryId != null) {
                return hasCursor
                        ? videoRepository.findByChannelAndCategoryWithCursorDesc(channelId, categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByChannelAndCategoryWithoutCursorDesc(channelId, categoryId, pageable);
            }
            if (channelId != null) {
                return hasCursor
                        ? videoRepository.findByChannelWithCursorDesc(channelId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByChannelWithoutCursorDesc(channelId, pageable);
            }
            if (categoryId != null) {
                return hasCursor
                        ? videoRepository.findByCategoryWithCursorDesc(categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByCategoryWithoutCursorDesc(categoryId, pageable);
            }
            return hasCursor
                    ? videoRepository.findAllPublicWithCursorDesc(cursorTimestamp, cursorId, pageable)
                    : videoRepository.findAllPublicWithoutCursorDesc(pageable);
        } else {
            if (channelId != null && categoryId != null) {
                return hasCursor
                        ? videoRepository.findByChannelAndCategoryWithCursorAsc(channelId, categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByChannelAndCategoryWithoutCursorAsc(channelId, categoryId, pageable);
            }
            if (channelId != null) {
                return hasCursor
                        ? videoRepository.findByChannelWithCursorAsc(channelId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByChannelWithoutCursorAsc(channelId, pageable);
            }
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

    private List<Video> fetchVideosByIds(List<UUID> ids, boolean isForward, Long categoryId,
                                         LocalDateTime cursorTimestamp, UUID cursorId, Pageable pageable) {
        boolean hasCursor = cursorTimestamp != null && cursorId != null;

        if (isForward) {
            if (categoryId != null) {
                return hasCursor
                        ? videoRepository.findByIdsAndCategoryWithCursorDesc(ids, categoryId, cursorTimestamp, cursorId, pageable)
                        : videoRepository.findByIdsAndCategoryWithoutCursorDesc(ids, categoryId, pageable);
            }
            return hasCursor
                    ? videoRepository.findByIdsWithCursorDesc(ids, cursorTimestamp, cursorId, pageable)
                    : videoRepository.findByIdsWithoutCursorDesc(ids, pageable);
        }

        if (categoryId != null) {
            return hasCursor
                    ? videoRepository.findByIdsAndCategoryWithCursorAsc(ids, categoryId, cursorTimestamp, cursorId, pageable)
                    : videoRepository.findByIdsAndCategoryWithoutCursorAsc(ids, categoryId, pageable);
        }
        return hasCursor
                ? videoRepository.findByIdsWithCursorAsc(ids, cursorTimestamp, cursorId, pageable)
                : videoRepository.findByIdsWithoutCursorAsc(ids, pageable);
    }

    @Transactional
    @Override
    public void updateVideoProcessingStatus(String videoId, String status, String processedPath, String[] resolutions, Long duration, String fileName, Long fileSize, String fileType) {
        Video video = videoRepository.findById(UUID.fromString(videoId))
                .orElseThrow(() -> new InvalidVideoRequestException("Video with ID " + videoId + " not found"));

        final var videoProcessing = this.videoProcessingService.findById(UUID.fromString(videoId));

        final var isCompleted = status.equals("completed");


        if(!isCompleted) {
            log.info("Video failed to be encoded {}", videoId);
        }

        if (resolutions != null) {
            video.setResolutions(String.join(",", resolutions));
        }

        if (processedPath != null && !processedPath.isBlank()) {
            videoProcessing.setUploadKey(processedPath);
        }

        videoProcessing.setDurationSeconds(duration);
        videoProcessing.setFileName(fileName);
        videoProcessing.setFileSizeBytes(fileSize);
        videoProcessing.setFileType(fileType);

        videoRepository.save(video);
    }

    @Override
    @Transactional
    public void updateVideoThumbnail(String videoId, String thumbnailUrl) {
        UUID id = UUID.fromString(videoId);
        String resolvedUrl = resolveThumbnailUrl(thumbnailUrl);

        final var isCustomThumbnail = resolvedUrl.contains("custom");

        if (isCustomThumbnail) {
            log.info("Custom thumbnail url found {}", resolvedUrl);
        }

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

    private VideoDto toDto(Video video, VideoProcessing videoProcessing, Boolean likedByMe, Set<VideoField> fields, boolean isOwner, Long effectiveViewCount) {
        Map<String, Thumbnail> thumbnails = fields.contains(VideoField.THUMBNAILS) && video.getThumbnailUrl() != null
                ? buildThumbnailMap(video.getThumbnailUrl())
                : null;

        ContentDetails contentDetails = fields.contains(VideoField.CONTENT_DETAILS)
                ? new ContentDetails(
                video.getResolutions(),
                videoProcessing.getDurationSeconds() != null ? videoProcessing.getDurationSeconds() : 0L,
                videoProcessing.getUploadKey(),
                video.getDefaultLanguage(),
                video.getPublishedLocale(),
                video.getOnlyForAdults())
                : null;

        Statistics statistics = fields.contains(VideoField.STATISTICS)
                ? new Statistics(
                video.getLikeCount(),
                effectiveViewCount,
                video.getCommentCount())
                : null;

        ProcessingDetails processingDetails = fields.contains(VideoField.PROCESSING_DETAILS)
            ? new ProcessingDetails(
                videoProcessing.getTranscodingProgress(),
                videoProcessing.getTranscodingStatus().getValue(),
                videoProcessing.getFailureReason()
            ) : null;

        FileDetails fileDetails = (fields.contains(VideoField.FILE_DETAILS) && isOwner)
                ? new FileDetails(videoProcessing.getFileName(), videoProcessing.getFileSizeBytes(), videoProcessing.getFileType())
                : null;

        Channel channel = fields.contains(VideoField.CHANNEL)
                ? resolveChannel(video)
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

    private Channel resolveChannel(Video video) {
        UUID creatorId = video.getCreatorId();
        if (creatorId == null) {
            return new Channel(null, null, null);
        }
        if (video.getCreatorDisplayName() != null) {
            return new Channel(creatorId.toString(), video.getCreatorDisplayName(), video.getCreatorAvatarUrl());
        }
        try {
            CreatorResponse creator = userServiceClient.getCreatorById(creatorId);
            video.setCreatorDisplayName(creator.displayName());
            video.setCreatorAvatarUrl(creator.avatar());
            videoRepository.save(video);
            return new Channel(creatorId.toString(), creator.displayName(), creator.avatar());
        } catch (Exception e) {
            log.warn("Could not resolve channel info for creatorId {}: {}", creatorId, e.getMessage());
            return new Channel(creatorId.toString(), null, null);
        }
    }

    private Map<String, Boolean> resolveLikedByMeBatch(List<UUID> videoIds, UUID userId) {
        if (userId == null || videoIds == null || videoIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, String> reactions = interactionServiceClient.getBatchReactions(videoIds, userId);
            Map<String, Boolean> result = new HashMap<>();
            reactions.forEach((vid, reactionType) -> result.put(vid, "like".equalsIgnoreCase(reactionType)));
            return result;
        } catch (Exception e) {
            log.debug("Could not fetch batch reactions for userId={}", userId);
            return Map.of();
        }
    }

    private boolean isOwner(Video video, UUID userId) {
        return userId != null && userId.equals(video.getCreatorId());
    }

    private Map<String, Thumbnail> buildThumbnailMap(String baseThumbnailUrl) {
        Map<String, Thumbnail> thumbnails = new LinkedHashMap<>();
        for (ThumbnailProfile profile : THUMBNAIL_PROFILES) {
            thumbnails.put(
                    profile.profile(),
                    new Thumbnail(
                            baseThumbnailUrl + "/" + profile.profile() + ".jpg",
                            profile.width(),
                            profile.height()
                    )
            );
        }

        return thumbnails;
    }

    private String encodeCursor(LocalDateTime timestamp, UUID id, String direction) {
        return PageTokenUtil.encode("createdAt", timestamp.toString(), "id", id.toString(), "direction", direction);
    }
}
