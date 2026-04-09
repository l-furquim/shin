package com.shin.streaming.service;


import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;

import java.util.UUID;

public interface StorageService {

    CookiesForCustomPolicy generateSignedCookiesForVideo(UUID videoId);

}
