package com.zkry.trip.dto.ai;

import java.util.List;

public record RecommendRequest(
    String budgetLevel,
    String travelStyle,
    String preferredCity,
    List<String> preferredTags,
    String transportation,
    String hotelLevel,
    String dietPreference,
    String type,
    String cityFilter,
    Integer limit,
    List<Integer> excludeIds
) {
}
