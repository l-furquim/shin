package com.shin.metadata.controller;

import com.shin.metadata.dto.CreateVideoRequest;
import com.shin.metadata.dto.CreateVideoResponse;
import com.shin.metadata.dto.GetVideoByIdResponse;
import com.shin.metadata.dto.PatchVideoByIdRequest;
import com.shin.metadata.dto.PatchVideoByIdResponse;
import com.shin.metadata.dto.SearchVideosResponse;
import com.shin.metadata.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${api.version}/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping
    public ResponseEntity<CreateVideoResponse> createVideo(
        @Valid @RequestBody CreateVideoRequest request
    ) {
        CreateVideoResponse response = videoService.createVideo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetVideoByIdResponse> getVideoById(
        @PathVariable("id") UUID id
    ) {
        GetVideoByIdResponse response = videoService.getVideoById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PatchVideoByIdResponse> patchVideoById(
        @PathVariable("id") UUID id,
        @RequestBody PatchVideoByIdRequest request
    ) {
        PatchVideoByIdResponse response = videoService.patchVideoById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(
        @PathVariable("id") UUID id
    ) {
        videoService.deleteVideoById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<SearchVideosResponse> search(
        @RequestParam(name = "accountId", required = false) String accountId,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "start", defaultValue = "0") int start,
        @RequestParam(name = "end", defaultValue = "10") int end
    ) {
        SearchVideosResponse response;

        if (accountId != null) {
            response = videoService.searchByAccountId(accountId, start, end);
        } else if (category != null) {
            response = videoService.searchByCategory(category, start, end);
        } else {
            response = videoService.searchAll(start, end);
        }

        if (response.videos().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
}
