package com.shin.metadata.controller;

import com.shin.metadata.dto.*;
import com.shin.metadata.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("${api.version}/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/init")
    public ResponseEntity<InitVideoResponse> initVideo(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.initVideo(userId));
    }

    @PostMapping
    public ResponseEntity<CreateVideoResponse> createVideo(
            @Valid @RequestBody CreateVideoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.createVideo(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDto> getVideoById(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(name = "fields", required = false) String fields
    ) {
        Set<VideoField> requestedFields = VideoField.parse(fields);
        return ResponseEntity.ok(videoService.getVideoById(id, userId, requestedFields));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VideoDto> patchVideoById(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PatchVideoByIdRequest request
    ) {
        return ResponseEntity.ok(videoService.patchVideoById(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(
            @PathVariable("id") UUID id
    ) {
        videoService.deleteVideoById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/watch")
    public ResponseEntity<WatchVideoResponse> getWatchVideoById(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(videoService.getWatchVideoById(id));
    }

    @GetMapping
    public ResponseEntity<SearchVideosResponse> search(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "fields", required = true) String fields,
            @RequestParam(name = "myRating", required = false) String myRating,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        Set<VideoField> requestedFields = VideoField.parse(fields);
        SearchVideosRequest request = new SearchVideosRequest(id, fields, myRating, categoryId, cursor, limit);
        return ResponseEntity.ok(videoService.search(request, userId, requestedFields));
    }
}
