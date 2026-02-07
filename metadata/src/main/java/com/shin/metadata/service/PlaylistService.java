package com.shin.metadata.service;

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

import java.util.UUID;

public interface PlaylistService {

    CreatePlaylistResponse create(CreatePlaylistRequest createPlaylistRequest);

    ReorderPlaylistResponse reorder(UUID playlistId, ReorderPlaylistRequest request);

    void delete(UUID playlistId);

    AddVideoToPlaylistResponse addVideo(UUID playlistId, AddVideoToPlaylistRequest request);

    GetPlaylistByIdResponse getById(UUID playlistId);

    PatchPlaylistResponse patchPlaylist(UUID playlistId, PatchPlaylistRequest request);

    RemoveVideoFromPlaylistResponse removeVideo(UUID playlistId, UUID videoId);
}
