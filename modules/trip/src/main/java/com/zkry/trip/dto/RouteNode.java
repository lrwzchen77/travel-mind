package com.zkry.trip.dto;

import java.util.List;

public record RouteNode(
    Integer order,
    String type,
    String poi_id,
    String name,
    Double longitude,
    Double latitude,
    String kind,
    String note,
    List<String> preferences
) {
    public RouteNode(
        Integer order, String type, String poi_id, String name,
        Double longitude, Double latitude, String kind
    ) {
        this(order, type, poi_id, name, longitude, latitude, kind, null, List.of());
    }

    public List<String> safePreferences() {
        return preferences == null ? List.of() : preferences;
    }
}
