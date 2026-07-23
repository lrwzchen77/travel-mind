package com.zkry.map.dto;

import java.util.List;

/** 面向消费者地图的结构化公开数据；缺失项用 availability 明示，不伪造。 */
public record PublicTravelMapSnapshot(
    String city,
    Weather weather,
    List<Place> places,
    Route route,
    Airport airport,
    RailwayCheck railway_check,
    Availability availability
) {
    public PublicTravelMapSnapshot withPlaces(List<Place> value) {
        List<Place> safe = value == null ? List.of() : value;
        return new PublicTravelMapSnapshot(city, weather, safe, route, airport, railway_check,
            new Availability(availability.weather(), safe.isEmpty() ? "unavailable" : "available", availability.routes()));
    }

    public static PublicTravelMapSnapshot empty(String city) {
        return new PublicTravelMapSnapshot(city, null, List.of(), null, null,
            new RailwayCheck("https://www.12306.cn/index/", "仅跳转 12306 官网核验，不提供车次、余票或票价。"),
            new Availability("unavailable", "unavailable", "unavailable"));
    }

    public record Weather(
        double temperature,
        String condition,
        double wind_speed,
        String updated_at,
        List<MapWeatherForecast> daily,
        String source
    ) {
    }

    public record Place(
        String id,
        String name,
        String kind,
        double longitude,
        double latitude,
        String address,
        String category,
        String opening_hours,
        double distance_km,
        Double rating,
        Double cost,
        String image_url,
        String tags,
        int community_mentions,
        String community_tip,
        String source,
        String updated_at
    ) {
    }

    public record Route(
        String from,
        String to,
        double distance_km,
        long duration_minutes,
        List<MapPoint> geometry,
        String source,
        String updated_at,
        String notice
    ) {
    }

    public record Airport(
        String code,
        String name,
        double longitude,
        double latitude,
        String source,
        String notice
    ) {
    }

    public record RailwayCheck(String url, String notice) {
    }

    public record Availability(String weather, String places, String routes) {
    }
}
