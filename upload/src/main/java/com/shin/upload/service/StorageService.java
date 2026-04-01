package com.shin.upload.service;

import com.shin.upload.dto.PresignedUpload;

import java.util.List;
import java.util.Map;

public interface StorageService {

    void upload(
        String bucket,
        String key,
        byte[] data,
        String contentType
    );

    void assembleChunks(
        List<String> sourceKeys,
        String destBucket,
        String destKey
    );

    void assembleChunks(
        List<String> sourceKeys,
        String destBucket,
        String destKey,
        Map<String, String> metadata
    );

    void delete(
        String bucket,
        String key
    );

    void deleteMultiple(
        String bucket,
        List<String> keys
    );

    PresignedUpload generaePresignedUpload(
        String bucket,
        String key,
        String contentType
    );

    PresignedUpload generaePresignedUpload(
        String bucket,
        String key,
        String contentType,
        String videoId,
        String userId,
        String originalName,
        Long fileSize,
        List<String> resolutions
    );

}
