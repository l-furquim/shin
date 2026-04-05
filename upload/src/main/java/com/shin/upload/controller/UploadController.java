package com.shin.upload.controller;

import com.shin.upload.dto.*;
import com.shin.upload.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${api.version}/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody InitiateUploadRequest request
    ) {
        InitiateUploadResponse response = uploadService.initiateUpload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/sessions/{uploadId}/chunks/{chunkNumber}")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
        @PathVariable String uploadId,
        @PathVariable Integer chunkNumber,
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        ChunkUploadResponse response = uploadService.uploadChunk(
            uploadId,
            chunkNumber,
            file.getBytes()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{uploadId}/complete")
    public ResponseEntity<RawUploadResponse> completeUpload(@PathVariable String uploadId) {
        RawUploadResponse response = uploadService.completeUpload(uploadId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/sessions/{uploadId}")
    public CancelUploadResponse cancelUpload(@PathVariable String uploadId) {
        return uploadService.cancelUpload(uploadId);
    }
}
