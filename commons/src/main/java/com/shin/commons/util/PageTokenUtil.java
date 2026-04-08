package com.shin.commons.util;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PageTokenUtil {

    private PageTokenUtil() {}

    public static String encode(String... kvPairs) {
        if (kvPairs.length == 0 || kvPairs.length % 2 != 0) {
            throw new IllegalArgumentException("kvPairs must be a non-empty even-length array");
        }
        var sb = new StringBuilder();
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) sb.append('|');
            sb.append(kvPairs[i]).append(':').append(kvPairs[i + 1]);
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            Map<String, String> result = new LinkedHashMap<>();
            for (String part : raw.split("\\|")) {
                String[] kv = part.split(":", 2);
                result.put(kv[0], kv[1]);
            }
            return result;
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.VALIDATION_FAILED, "Invalid pageToken");
        }
    }
}
