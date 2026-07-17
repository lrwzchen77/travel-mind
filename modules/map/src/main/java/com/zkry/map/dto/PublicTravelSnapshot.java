package com.zkry.map.dto;

import java.util.List;

public record PublicTravelSnapshot(
    List<MapWeatherForecast> weather,
    List<PublicDataItem> items
) {
    public static PublicTravelSnapshot empty() {
        return new PublicTravelSnapshot(List.of(), List.of());
    }

    public List<MapWeatherForecast> safeWeather() {
        return weather == null ? List.of() : weather;
    }

    public List<PublicDataItem> safeItems() {
        return items == null ? List.of() : items;
    }
}
