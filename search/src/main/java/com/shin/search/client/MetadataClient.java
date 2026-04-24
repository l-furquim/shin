package com.shin.search.client;

import com.shin.search.dto.SearchVideosResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "metadata-service")
public interface MetadataClient {

    @GetMapping("/api/v1/videos")
    ResponseEntity<SearchVideosResponse> search(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "ids", required = false) String ids,
            @RequestParam(name = "channelId", required = false) UUID channelId,
            @RequestParam(name = "fields", required = true) String fields,
            @RequestParam(name = "myRating", required = false) String myRating,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "forMine", required = false, defaultValue = "false") boolean forMine,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    );

}
