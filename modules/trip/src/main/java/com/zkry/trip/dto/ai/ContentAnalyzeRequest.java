package com.zkry.trip.dto.ai;

public record ContentAnalyzeRequest(
    String text,
    String city,
    String attraction_name,
    String language
) {
}
