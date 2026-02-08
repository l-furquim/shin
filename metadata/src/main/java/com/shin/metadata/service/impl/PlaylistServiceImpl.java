package com.shin.metadata.service.impl;

import com.shin.metadata.dto.*;
import com.shin.metadata.exception.InvalidPlaylistRequestException;
import com.shin.metadata.exception.PlaylistNotFoundException;
import com.shin.metadata.model.Playlist;
import com.shin.metadata.repository.PlaylistRepository;
import com.shin.metadata.service.PlaylistService;
import com.shin.metadata.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private static final String PLAYLIST_NOT_FOUND = "Playlist with id: %s not found";
    private static final String VIDEO_NOT_IN_PLAYLIST = "Video with id: %s not found in playlist with id: %s";

    private final PlaylistRepository playlistRepository;
    private final VideoService videoService;

    @Override
    @Transactional
    public CreatePlaylistResponse create(CreatePlaylistRequest request) {
        validateTitle(request.title());

        List<UUID> videoIds = request.videos() != null ? new ArrayList<>(request.videos()) : new ArrayList<>();
        String thumbnailUrl = determineThumbnailUrl(videoIds);

        Playlist playlist = Playlist.builder()
            .id(UUID.randomUUID())
            .name(request.title())
            .description(request.description())
            .thumbnailUrl(thumbnailUrl)
            .visibility(request.visibility())
            .videos(videoIds)
            .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("Created playlist: {}", saved.getId());

        return toCreateResponse(saved);
    }

    @Override
    @Transactional
    public ReorderPlaylistResponse reorder(UUID playlistId, ReorderPlaylistRequest request) {
        Playlist playlist = findPlaylistById(playlistId);

        validateReorderRequest(request, playlist);

        List<UUID> currentVideos = playlist.getVideos();
        List<UUID> newOrder = new ArrayList<>(request.videosIds());

        if (!haveSameElements(currentVideos, newOrder)) {
            throw new InvalidPlaylistRequestException(
                "New order must contain exactly the same videos as current playlist"
            );
        }

        playlist.setVideos(newOrder);
        Playlist saved = playlistRepository.save(playlist);

        log.info("Reordered playlist: {}", playlistId);
        return toReorderResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID playlistId) {
        Playlist playlist = findPlaylistById(playlistId);
        playlistRepository.delete(playlist);
        log.info("Deleted playlist: {}", playlistId);
    }

    @Override
    @Transactional
    public AddVideoToPlaylistResponse addVideo(UUID playlistId, AddVideoToPlaylistRequest request) {
        Playlist playlist = findPlaylistById(playlistId);

        if (request.videos() == null || request.videos().isEmpty()) {
            throw new InvalidPlaylistRequestException("Videos list cannot be empty");
        }

        List<UUID> playlistVideos = playlist.getVideos();

        for (VideoToAdd videoToAdd : request.videos()) {
            validateVideoToAdd(videoToAdd);

            UUID videoId = videoToAdd.videoId();

            if (playlistVideos.contains(videoId)) {
                log.warn("Video {} already in playlist {}, skipping", videoId, playlistId);
                continue;
            }

            int position = videoToAdd.position() != null ? videoToAdd.position() : playlistVideos.size();

            if (position < 0 || position > playlistVideos.size()) {
                throw new InvalidPlaylistRequestException(
                    String.format("Invalid position %d. Must be between 0 and %d", position, playlistVideos.size())
                );
            }

            playlistVideos.add(position, videoId);
        }

        Playlist saved = playlistRepository.save(playlist);
        log.info("Added {} videos to playlist: {}", request.videos().size(), playlistId);

        return toAddVideoResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GetPlaylistByIdResponse getById(UUID playlistId) {
        Playlist playlist = findPlaylistById(playlistId);
        return toGetByIdResponse(playlist);
    }

    @Override
    @Transactional
    public PatchPlaylistResponse patchPlaylist(UUID playlistId, PatchPlaylistRequest request) {
        Playlist playlist = findPlaylistById(playlistId);

        boolean updated = false;

        if (request.videos() != null) {
            playlist.setVideos(new ArrayList<>(request.videos()));
            updated = true;
        }
        if (request.name() != null && !request.name().isBlank()) {
            playlist.setName(request.name());
            updated = true;
        }
        if (request.description() != null) {
            playlist.setDescription(request.description());
            updated = true;
        }
        if (request.visibility() != null) {
            playlist.setVisibility(request.visibility());
            updated = true;
        }
        if (request.thumbnailUrl() != null) {
            playlist.setThumbnailUrl(request.thumbnailUrl());
            updated = true;
        }

        if (!updated) {
            log.debug("No fields to update for playlist: {}", playlistId);
            return toPatchResponse(playlist);
        }

        Playlist saved = playlistRepository.save(playlist);
        log.info("Updated playlist: {}", playlistId);

        return toPatchResponse(saved);
    }

    @Override
    @Transactional
    public RemoveVideoFromPlaylistResponse removeVideo(UUID playlistId, UUID videoId) {
        Playlist playlist = findPlaylistById(playlistId);

        boolean removed = playlist.getVideos().remove(videoId);

        if (!removed) {
            throw new InvalidPlaylistRequestException(
                String.format(VIDEO_NOT_IN_PLAYLIST, videoId, playlistId)
            );
        }

        Playlist saved = playlistRepository.save(playlist);
        log.info("Removed video {} from playlist: {}", videoId, playlistId);

        return toRemoveVideoResponse(saved);
    }

    private Playlist findPlaylistById(UUID playlistId) {
        return playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException(
                String.format(PLAYLIST_NOT_FOUND, playlistId)
            ));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidPlaylistRequestException("Title cannot be empty");
        }
    }

    private void validateReorderRequest(ReorderPlaylistRequest request, Playlist playlist) {
        if (request.videosIds() == null || request.videosIds().isEmpty()) {
            throw new InvalidPlaylistRequestException("videosIds cannot be null or empty");
        }

        if (request.videosIds().size() != playlist.getVideos().size()) {
            throw new InvalidPlaylistRequestException(
                String.format("videosIds size (%d) must match playlist size (%d)",
                    request.videosIds().size(), playlist.getVideos().size())
            );
        }
    }

    private void validateVideoToAdd(VideoToAdd videoToAdd) {
        if (videoToAdd == null || videoToAdd.videoId() == null) {
            throw new InvalidPlaylistRequestException("Video ID cannot be null");
        }
    }

    private boolean haveSameElements(List<UUID> list1, List<UUID> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        Set<UUID> set1 = new HashSet<>(list1);
        Set<UUID> set2 = new HashSet<>(list2);
        return set1.equals(set2);
    }

    private String determineThumbnailUrl(List<UUID> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return "";
        }

        try {
            var video = videoService.getVideoById(videoIds.getFirst());
            return video.thumbnailUrl() != null ? video.thumbnailUrl() : "";
        } catch (Exception e) {
            log.warn("Failed to get thumbnail from video: {}", videoIds.getFirst(), e);
            return "";
        }
    }

    private CreatePlaylistResponse toCreateResponse(Playlist playlist) {
        return new CreatePlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    private ReorderPlaylistResponse toReorderResponse(Playlist playlist) {
        return new ReorderPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    private AddVideoToPlaylistResponse toAddVideoResponse(Playlist playlist) {
        return new AddVideoToPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    private GetPlaylistByIdResponse toGetByIdResponse(Playlist playlist) {
        return new GetPlaylistByIdResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    private PatchPlaylistResponse toPatchResponse(Playlist playlist) {
        return new PatchPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    private RemoveVideoFromPlaylistResponse toRemoveVideoResponse(Playlist playlist) {
        return new RemoveVideoFromPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }
}
