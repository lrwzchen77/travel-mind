package com.zkry.map.dto;

public record MapPoi(
    String id,
    String name,
    String address,
    MapPoint location,
    String type,
    String rating,
    String distance,
    String photoUrl,
    String openTimeWeek,
    String openTimeToday,
    String cost,
    String tag
) {
}
