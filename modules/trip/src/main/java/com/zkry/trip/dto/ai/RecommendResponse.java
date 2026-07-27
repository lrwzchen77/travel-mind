package com.zkry.trip.dto.ai;

import java.util.List;

public record RecommendResponse(
    List<RecommendResult> results,
    String type,
    Boolean fallback
) {
}
