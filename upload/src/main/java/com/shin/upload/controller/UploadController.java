package com.shin.upload.controller;

import com.shin.upload.dto.*;
import com.shin.upload.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${api.version}/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/video/raw")
    public ResponseEntity<RawUploadResponse> uploadRawVideo(
        @RequestHeader("X-User-Id") String userId,
        @RequestPart("data") RawUploadData data,
        @RequestPart("file") MultipartFile file
    ) {
        RawUploadResponse response = uploadService.uploadRawVideo(
            userId,
            data,
            file
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/video/initiate")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody InitiateUploadRequest request
    ) {
        InitiateUploadResponse response = uploadService.initiateUpload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/video/chunk")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
        @RequestParam String uploadId,
        @RequestParam Integer chunkNumber,
        @RequestParam Integer totalChunks,
        @RequestParam MultipartFile file
    ) throws IOException {
        ChunkUploadResponse response = uploadService.uploadChunk(
            uploadId,
            chunkNumber,
            totalChunks,
            file.getBytes()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/video/cancel")
    public CancelUploadResponse cancelUpload(@RequestParam String uploadId) {
        return uploadService.cancelUpload(uploadId);
    }
}
