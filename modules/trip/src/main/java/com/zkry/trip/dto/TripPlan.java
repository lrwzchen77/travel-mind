package com.zkry.trip.dto;

import com.zkry.map.dto.PublicDataItem;
import java.util.List;

public record TripPlan(
    String city,
    List<String> cities,
    String start_date,
    String end_date,
    List<DayPlan> days,
    List<WeatherInfo> weather_info,
    String overall_suggestions,
    Budget budget,
    List<InspirationSource> inspiration_sources,
    List<PublicDataItem> public_data,
    RouteIntent route_intent
) {
    public TripPlan(
        String city, List<String> cities, String start_date, String end_date, List<DayPlan> days,
        List<WeatherInfo> weather_info, String overall_suggestions, Budget budget
    ) {
        this(city, cities, start_date, end_date, days, weather_info, overall_suggestions, budget, List.of(), List.of(), null);
    }

    public TripPlan(
        String city, List<String> cities, String start_date, String end_date, List<DayPlan> days,
        List<WeatherInfo> weather_info, String overall_suggestions, Budget budget,
        List<InspirationSource> inspiration_sources
    ) {
        this(city, cities, start_date, end_date, days, weather_info, overall_suggestions, budget,
            inspiration_sources, List.of(), null);
    }

    public TripPlan(
        String city, List<String> cities, String start_date, String end_date, List<DayPlan> days,
        List<WeatherInfo> weather_info, String overall_suggestions, Budget budget,
        List<InspirationSource> inspiration_sources, List<PublicDataItem> public_data
    ) {
        this(city, cities, start_date, end_date, days, weather_info, overall_suggestions, budget,
            inspiration_sources, public_data, null);
    }
}
