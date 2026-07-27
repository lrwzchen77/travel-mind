package com.zkry.trip.dto.ai;

import java.util.List;

public record RecommendResult(
    Integer itemId,
    String itemType,
    String name,
    String city,
    String description,
    List<String> tags,
    Double rating,
    Integer popularity,
    Double score,
    String matchReason
) {
}
