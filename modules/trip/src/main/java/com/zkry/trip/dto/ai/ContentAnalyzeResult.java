package com.zkry.trip.dto.ai;

import java.util.List;

public record ContentAnalyzeResult(
    String sentiment,
    List<String> keywords,
    List<String> positive_highlights,
    List<String> negative_warnings,
    List<String> suitable_traveler_types
) {
}
