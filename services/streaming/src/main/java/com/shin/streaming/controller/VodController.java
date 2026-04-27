package com.shin.streaming.controller;

import com.shin.streaming.dto.ViewEventRequest;
import com.shin.streaming.dto.WatchVodResponse;
import com.shin.streaming.service.VodService;
import com.shin.streaming.dto.WatchVodResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/vod")
public class VodController {

    private final VodService vodService;

    @GetMapping
    public ResponseEntity<WatchVodResponse> watchVod(
            @RequestParam(value = "videoId") UUID videoId,
            @RequestParam(value = "resolutions") String resolutions,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {

        final var formattedResolutions = resolutions.split(",");

        if (formattedResolutions.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        WatchVodResult result = vodService.watchVod(userId, videoId, videoUrl, formattedResolutions);

        HttpHeaders headers = new HttpHeaders();
        result.cookies().forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie));

        return ResponseEntity.ok().headers(headers).body(result.response());
    }

    @PostMapping("/playback")
    public ResponseEntity<Void> playbackEvent(
            @RequestBody ViewEventRequest viewEventRequest,
            @RequestHeader("X-User-Id") UUID userId
    ) {
       this.vodService.handlePlaybackEvent(viewEventRequest,userId);

       return ResponseEntity.ok().build();
    }

}
