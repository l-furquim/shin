package com.shin.streaming.client;

import com.shin.streaming.dto.VideoDetails;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "metadata-service", url = "${clients.metadata-service.url}")
public interface MetadataServiceClient {

    @GetMapping("/api/v1/videos/{id}/watch")
    VideoDetails getWatchVideoById(@PathVariable("id") UUID id);

}
