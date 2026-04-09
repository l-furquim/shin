package com.shin.streaming.controller;

import com.shin.streaming.dto.WatchVodResponse;
import com.shin.streaming.service.VodService;
import com.shin.streaming.dto.WatchVodResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.version}/watch")
public class VodController {

    private final VodService vodService;

    @GetMapping
    public ResponseEntity<WatchVodResponse> watchVod(
            @RequestParam(value = "videoId") UUID videoId,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        WatchVodResult result = vodService.watchVod(userId, videoId, videoUrl);

        HttpHeaders headers = new HttpHeaders();
        result.cookies().forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie));

        return ResponseEntity.ok().headers(headers).body(result.response());
    }

}
