package com.zkry.trip.dto.ai;

public record VisionDetectRequest(
    String image_url,
    String city,
    String resource_type
) {
}
