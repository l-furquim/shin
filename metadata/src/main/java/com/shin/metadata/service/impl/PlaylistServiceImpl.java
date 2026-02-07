package com.shin.metadata.service.impl;

import com.shin.metadata.dto.AddVideoToPlaylistRequest;
import com.shin.metadata.dto.AddVideoToPlaylistResponse;
import com.shin.metadata.dto.CreatePlaylistRequest;
import com.shin.metadata.dto.CreatePlaylistResponse;
import com.shin.metadata.dto.GetPlaylistByIdResponse;
import com.shin.metadata.dto.PatchPlaylistRequest;
import com.shin.metadata.dto.PatchPlaylistResponse;
import com.shin.metadata.dto.RemoveVideoFromPlaylistResponse;
import com.shin.metadata.dto.ReorderPlaylistRequest;
import com.shin.metadata.dto.ReorderPlaylistResponse;
import com.shin.metadata.dto.VideoToAdd;
import com.shin.metadata.exception.InvalidPlaylistRequestException;
import com.shin.metadata.exception.PlaylistNotFoundException;
import com.shin.metadata.model.Playlist;
import com.shin.metadata.repository.PlaylistRepository;
import com.shin.metadata.service.PlaylistService;
import com.shin.metadata.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final VideoService videoService;

    @Override
    public CreatePlaylistResponse create(CreatePlaylistRequest createPlaylistRequest) {
        if (createPlaylistRequest.title().isBlank()) {
            throw new InvalidPlaylistRequestException("Title must not be empty");
        }

        String thumbnailUrl = "";
        if (createPlaylistRequest.videos() != null && !createPlaylistRequest.videos().isEmpty()) {
            UUID firstVideoId = createPlaylistRequest.videos().get(0);
            var video = videoService.getVideoById(firstVideoId);
            thumbnailUrl = video.thumbnailUrl() != null ? video.thumbnailUrl() : "";
        }

        Playlist playlist = Playlist.builder()
            .id(UUID.randomUUID())
            .name(createPlaylistRequest.title())
            .description(createPlaylistRequest.description())
            .thumbnailUrl(thumbnailUrl)
            .visibility(createPlaylistRequest.visibility())
            .videos(createPlaylistRequest.videos() != null ? new ArrayList<>(createPlaylistRequest.videos()) : new ArrayList<>())
            .build();

        Playlist saved = playlistRepository.save(playlist);

        return new CreatePlaylistResponse(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getThumbnailUrl(),
            saved.getVisibility(),
            saved.getVideos()
        );
    }

    @Override
    public ReorderPlaylistResponse reorder(UUID playlistId, ReorderPlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        if (request.videosIds().isEmpty()) {
            throw new InvalidPlaylistRequestException("videosIds must not be null");
        }

        List<UUID> newOrder = request.videosIds();
        List<UUID> currentVideos = playlist.getVideos();

        int size = Math.min(currentVideos.size(), newOrder.size());
        for (int i = 0; i < size; i++) {
            if (!currentVideos.get(i).equals(newOrder.get(i))) {
                currentVideos.set(i, newOrder.get(i));
            }
        }

        if (newOrder.size() > currentVideos.size()) {
            currentVideos.addAll(newOrder.subList(currentVideos.size(), newOrder.size()));
        } else if (newOrder.size() < currentVideos.size()) {
            for (int i = currentVideos.size() - 1; i >= newOrder.size(); i--) {
                currentVideos.remove(i);
            }
        }

        playlistRepository.save(playlist);

        return new ReorderPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    @Override
    public void delete(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        playlistRepository.delete(playlist);
    }

    @Override
    public AddVideoToPlaylistResponse addVideo(UUID playlistId, AddVideoToPlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        List<UUID> playlistVideos = playlist.getVideos();
        int size = Math.max(playlistVideos.size(), request.videos().size());

        for (int i = 0; i < size; i++) {
            VideoToAdd videoToAdd = i < request.videos().size() ? request.videos().get(i) : null;
            if (videoToAdd != null) {
                int position = videoToAdd.position() != null ? videoToAdd.position() : playlistVideos.size();
                if (position >= 0 && position <= playlistVideos.size()) {
                    playlistVideos.add(position, videoToAdd.videoId());
                } else {
                    throw new InvalidPlaylistRequestException("Invalid position " + videoToAdd.position() + " for video " + videoToAdd.videoId());
                }
            }
        }

        playlistRepository.save(playlist);

        return new AddVideoToPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    @Override
    public GetPlaylistByIdResponse getById(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        return new GetPlaylistByIdResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    @Override
    public PatchPlaylistResponse patchPlaylist(UUID playlistId, PatchPlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        if (request.videos() != null) {
            playlist.setVideos(new ArrayList<>(request.videos()));
        }
        if (request.name() != null) {
            playlist.setName(request.name());
        }
        if (request.description() != null) {
            playlist.setDescription(request.description());
        }
        if (request.visibility() != null) {
            playlist.setVisibility(request.visibility());
        }
        if (request.thumbnailUrl() != null) {
            playlist.setThumbnailUrl(request.thumbnailUrl());
        }

        playlistRepository.save(playlist);

        return new PatchPlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getThumbnailUrl(),
            playlist.getVisibility(),
            playlist.getVideos()
        );
    }

    @Override
    public RemoveVideoFromPlaylistResponse removeVideo(UUID playlistId, UUID videoId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new PlaylistNotFoundException("Playlist wth id: " + playlistId + " not found"));

        boolean removed = playlist.getVideos().remove(videoId);

        if (!removed) {
            throw new InvalidPlaylistRequestException("Video with id: " + videoId + " not found in playlist with id: " + playlistId);
        }

        playlistRepository.save(playlist);

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
