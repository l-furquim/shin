package com.shin.subscription.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.subscription.exceptions.InvalidSubscriptionException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CursorUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Prefixes encode the DynamoDB attribute type so we can reconstruct AttributeValue on decode
    private static final String S_PREFIX = "S:";
    private static final String N_PREFIX = "N:";

    private CursorUtils() {}

    static String encode(Map<String, AttributeValue> lastKey) {
        try {
            Map<String, String> serializable = lastKey.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().n() != null ? N_PREFIX + e.getValue().n() : S_PREFIX + e.getValue().s()
                    ));
            return Base64.getUrlEncoder().encodeToString(MAPPER.writeValueAsBytes(serializable));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode cursor", e);
        }
    }

    static Map<String, AttributeValue> decode(String cursor, Set<String> allowedKeys) {
        try {
            Map<String, String> raw = MAPPER.readValue(
                    Base64.getUrlDecoder().decode(cursor),
                    new TypeReference<>() {}
            );

            if (!raw.keySet().equals(allowedKeys)) {
                throw new InvalidSubscriptionException("Invalid pagination cursor");
            }

            return raw.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                String val = e.getValue();
                                if (val.startsWith(N_PREFIX)) {
                                    return AttributeValue.builder().n(val.substring(N_PREFIX.length())).build();
                                } else if (val.startsWith(S_PREFIX)) {
                                    return AttributeValue.builder().s(val.substring(S_PREFIX.length())).build();
                                }
                                throw new InvalidSubscriptionException("Invalid pagination cursor");
                            }
                    ));
        } catch (InvalidSubscriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSubscriptionException("Invalid pagination cursor");
        }
    }
}
