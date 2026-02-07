package com.shin.upload.service.impl;

import com.shin.upload.client.MetadataClient;
import com.shin.upload.dto.CreateVideoRequest;
import com.shin.upload.dto.UpdateVideoRequest;
import com.shin.upload.service.MetadataClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MetadataClientServiceImpl implements MetadataClientService {

    private static final Logger log = LoggerFactory.getLogger(MetadataClientServiceImpl.class);

    private final MetadataClient client;

    @Override
    public void createVideo(CreateVideoRequest request) {
        try {
            ResponseEntity<Object> response = client.createVideo(request);

            log.info("Response from metadata service: statusCode={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode() != HttpStatus.CREATED) {
                log.error("Creating video request failed: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed while requesting to create video: {}", e.getMessage());
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
