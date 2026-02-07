package com.shin.metadata.controller;

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
import com.shin.metadata.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${api.version}/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService service;

    @PostMapping
    public ResponseEntity<CreatePlaylistResponse> createPlaylist(
        @Valid @RequestBody CreatePlaylistRequest request
    ) {
        CreatePlaylistResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetPlaylistByIdResponse> getPlaylistById(
        @PathVariable UUID id
    ) {
        GetPlaylistByIdResponse response = service.getById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PatchPlaylistResponse> patchPlaylist(
        @PathVariable UUID id,
        @RequestBody PatchPlaylistRequest request
    ) {
        PatchPlaylistResponse response = service.patchPlaylist(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylistById(
        @PathVariable UUID id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/videos")
    public ResponseEntity<AddVideoToPlaylistResponse> addVideoToPlaylist(
        @PathVariable UUID id,
        @Valid @RequestBody AddVideoToPlaylistRequest request
    ) {
        AddVideoToPlaylistResponse response = service.addVideo(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/videos/{videoId}")
    public ResponseEntity<RemoveVideoFromPlaylistResponse> removeVideoFromPlaylist(
        @PathVariable UUID id,
        @PathVariable UUID videoId
    ) {
        RemoveVideoFromPlaylistResponse response = service.removeVideo(id, videoId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reorder")
    public ResponseEntity<ReorderPlaylistResponse> reorderPlaylist(
        @PathVariable UUID id,
        @Valid @RequestBody ReorderPlaylistRequest request
    ) {
        ReorderPlaylistResponse response = service.reorder(id, request);
        return ResponseEntity.ok(response);
    }
}
