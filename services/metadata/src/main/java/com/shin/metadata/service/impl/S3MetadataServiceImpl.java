package com.shin.metadata.service.impl;

import com.shin.metadata.service.S3MetadataService;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class S3MetadataServiceImpl implements S3MetadataService {

    private final S3Client s3Client;

    public S3MetadataServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Map<String, String> getObjectMetadata(String bucketName, String objectKey) {
        final var response = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build()
        );

        return new HashMap<>(response.metadata());
    }
}
