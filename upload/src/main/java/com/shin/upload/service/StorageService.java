package com.shin.upload.service;

import java.util.List;

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

    void delete(
        String bucket,
        String key
    );

    void deleteMultiple(
        String bucket,
        List<String> keys
    );
}
