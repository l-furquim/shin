package com.shin.metadata.controller;

import com.shin.metadata.dto.*;
import com.shin.metadata.service.VideoProcessingService;
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
    private final VideoProcessingService videoProcessingService;

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

    @PutMapping("/{id}/publish")
    public ResponseEntity<VideoDto> publish(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        this.videoService.publish(id, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<VideoProgressResponse> progress(
           @RequestHeader("X-User-Id") UUID userId,
           @PathVariable("id") UUID videoId
    ) {
        final var response = videoProcessingService.progress(userId, videoId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<SearchVideosResponse> search(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "ids", required = false) String ids,
            @RequestParam(name = "channelId", required = false) UUID channelId,
            @RequestParam(name = "fields", required = true) String fields,
            @RequestParam(name = "myRating", required = false) String myRating,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        Set<VideoField> requestedFields = VideoField.parse(fields);
        SearchVideosRequest request = new SearchVideosRequest(id, ids, channelId, fields, myRating, categoryId, cursor, limit);
        return ResponseEntity.ok(videoService.search(request, userId, requestedFields));
    }

}
