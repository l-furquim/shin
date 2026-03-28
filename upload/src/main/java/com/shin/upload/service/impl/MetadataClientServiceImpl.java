package com.shin.upload.service.impl;

import com.shin.upload.client.MetadataClient;
import com.shin.upload.dto.CreateVideoRequest;
import com.shin.upload.dto.CreateVideoResponse;
import com.shin.upload.dto.UpdateVideoRequest;
import com.shin.upload.service.MetadataClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MetadataClientServiceImpl implements MetadataClientService {

    private static final Logger log = LoggerFactory.getLogger(MetadataClientServiceImpl.class);

    private final MetadataClient client;

    public MetadataClientServiceImpl(MetadataClient client) {
        this.client = client;
    }

    @Override
    public CreateVideoResponse createVideo(CreateVideoRequest request) {
        try {
            ResponseEntity<CreateVideoResponse> response = client.createVideo(request);

            log.info("Response from metadata service: statusCode={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null || response.getBody().id() == null) {
                throw new IllegalStateException("Creating video request failed");
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed while requesting to create video: {}", e.getMessage());
            throw new IllegalStateException("Failed while requesting to create video", e);
        }
    }

    @Override
    public void updateVideo(UpdateVideoRequest request, String videoId) {
        try {
            ResponseEntity<Object> response = client.updateVideo(UUID.fromString(videoId), request);

            log.info("Response from metadata service while updating video: statusCode={}, body={}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Failed while requesting to update video: {}", e.getMessage());
        }
    }
}
