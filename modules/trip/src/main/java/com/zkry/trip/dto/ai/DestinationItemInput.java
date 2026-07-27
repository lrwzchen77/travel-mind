package com.zkry.trip.dto.ai;

import java.util.List;

public record DestinationItemInput(
    Integer itemId,
    String itemType,
    String name,
    String city,
    String description,
    List<String> tags,
    Double rating,
    Integer popularity
) {
}
