package com.shin.subscription.repository;

import java.util.List;

public record QueryPage<T>(List<T> items, String nextCursor) {
}
