package com.shin.upload.controller;

import com.shin.upload.dto.CancelUploadResponse;
import com.shin.upload.dto.ChunkUploadResponse;
import com.shin.upload.dto.InitiateUploadRequest;
import com.shin.upload.dto.InitiateUploadResponse;
import com.shin.upload.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${api.version}/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/video/initiate")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
        @Valid @RequestBody InitiateUploadRequest request
    ) {
        InitiateUploadResponse response = uploadService.initiateUpload(request);
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
