package com.shin.upload.controller;

import com.shin.upload.dto.*;
import com.shin.upload.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("${api.version}/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<RawUploadResponse> initiateRawUpload(
            @Valid @RequestBody RawUploadData data,
            @RequestHeader("X-User-Id") String userId
    ) {
        final var response = uploadService.initiateRawUpload(userId, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/thumbnail")
    public ResponseEntity<ThumbnailUploadResponse> thumbnailUpload(
            @Valid @RequestBody ThumbnailUploadRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        final var response = this.uploadService.thumbnailUpload(request, userId);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/chunked")
    public ResponseEntity<ChunkedUploadResponse> initiateChunkedUpload(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChunkedUploadRequest request
    ) {
        ChunkedUploadResponse response = uploadService.initiateUpload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/chunked/{uploadId}/complete")
    public ResponseEntity<CompleteUploadResponse> completeUpload(@PathVariable String uploadId) {
        CompleteUploadResponse response = uploadService.completeUpload(uploadId);
        return ResponseEntity.accepted().body(response);
    }

    @DeleteMapping("/chunked/{uploadId}")
    public ResponseEntity<Void> cancelUpload(@PathVariable String uploadId) {
        uploadService.cancelUpload(uploadId);
        return ResponseEntity.noContent().build();
    }
}
