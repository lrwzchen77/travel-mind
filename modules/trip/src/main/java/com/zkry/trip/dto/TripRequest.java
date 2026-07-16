package com.zkry.trip.dto;

import java.util.List;

public record TripRequest(
    String city,
    List<CityStay> cities,
    String start_date,
    String end_date,
    Integer travel_days,
    String transportation,
    String accommodation,
    String budget,
    List<String> preferences,
    String free_text_input,
    String language,
    List<Long> inspiration_ids,
    List<InspirationSource> inspiration_sources
) {
    public TripRequest(
        String city, List<CityStay> cities, String start_date, String end_date, Integer travel_days,
        String transportation, String accommodation, String budget, List<String> preferences,
        String free_text_input, String language
    ) {
        this(city, cities, start_date, end_date, travel_days, transportation, accommodation, budget, preferences,
            free_text_input, language, List.of(), List.of());
    }
    public List<CityStay> normalizedCities() {
        if (cities != null && !cities.isEmpty()) {
            return cities;
        }
        if (isBlank(city)) {
            return List.of();
        }
        return List.of(new CityStay(city, safeTravelDays()));
    }

    public int safeTravelDays() {
        if (travel_days != null && travel_days > 0) {
            return travel_days;
        }
        if (cities == null || cities.isEmpty()) {
            return 1;
        }
        return cities.stream().mapToInt(CityStay::safeDays).sum();
    }

    public String primaryCity() {
        List<CityStay> normalized = normalizedCities();
        return normalized.isEmpty() ? "" : normalized.get(0).city();
    }

    public List<String> safePreferences() {
        return preferences == null ? List.of() : preferences;
    }

    public String safeLanguage() {
        return isBlank(language) ? "zh" : language;
    }

    public String safeTransportation() {
        return isBlank(transportation) ? "公共交通" : transportation;
    }

    public String safeAccommodation() {
        return isBlank(accommodation) ? "舒适型酒店" : accommodation;
    }

    public String safeBudget() {
        return isBlank(budget) ? "未填写" : budget;
    }

    public List<Long> safeInspirationIds() {
        return inspiration_ids == null ? List.of() : inspiration_ids;
    }

    public List<InspirationSource> safeInspirationSources() {
        return inspiration_sources == null ? List.of() : inspiration_sources;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
