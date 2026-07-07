package com.zkry.resources.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class CrudResourceRegistry {

    private final Map<String, CrudResourceDefinition> definitions = new LinkedHashMap<>();

    public CrudResourceRegistry() {
        register(new CrudResourceDefinition(
            "users",
            "tm_user",
            "id",
            List.of("username", "nickname", "phone", "email"),
            List.of("status"),
            List.of("username", "nickname", "phone", "email", "avatar_url", "status")
        ));
        register(new CrudResourceDefinition(
            "user-preferences",
            "tm_user_preference",
            "id",
            List.of("budget_level", "travel_style", "preferred_city", "preferred_tags"),
            List.of("user_id"),
            List.of("user_id", "budget_level", "travel_style", "preferred_city", "preferred_tags", "transportation",
                "hotel_level", "diet_preference")
        ));
        register(new CrudResourceDefinition(
            "cities",
            "tm_city",
            "id",
            List.of("name", "province", "description"),
            List.of("status"),
            List.of("name", "province", "country", "description", "cover_image", "popularity", "status")
        ));
        register(new CrudResourceDefinition(
            "attractions",
            "tm_attraction",
            "id",
            List.of("name", "address", "description"),
            List.of("city_id", "category", "tags", "rating", "status"),
            List.of("city_id", "name", "category", "address", "description", "rating", "price", "tags",
                "opening_hours", "image_url", "status")
        ));
        register(new CrudResourceDefinition(
            "hotels",
            "tm_hotel",
            "id",
            List.of("name", "address", "description"),
            List.of("city_id", "category", "tags", "rating", "status"),
            List.of("city_id", "name", "category", "address", "description", "rating", "price_range", "tags",
                "image_url", "status")
        ));
        register(new CrudResourceDefinition(
            "restaurants",
            "tm_restaurant",
            "id",
            List.of("name", "cuisine", "address", "description"),
            List.of("city_id", "category", "tags", "rating", "status"),
            List.of("city_id", "name", "category", "cuisine", "address", "description", "rating", "average_cost",
                "tags", "image_url", "status")
        ));
        register(new CrudResourceDefinition(
            "travel-tags",
            "tm_travel_tag",
            "id",
            List.of("name", "category"),
            List.of("category", "status"),
            List.of("name", "category", "color", "status")
        ));
        register(new CrudResourceDefinition(
            "favorites",
            "tm_favorite",
            "id",
            List.of("target_type", "note"),
            List.of("user_id", "target_type", "target_id"),
            List.of("user_id", "target_type", "target_id", "note")
        ));
        register(new CrudResourceDefinition(
            "travel-notes",
            "tm_travel_note",
            "id",
            List.of("title", "content"),
            List.of("user_id", "city_id", "attraction_id", "status"),
            List.of("user_id", "city_id", "attraction_id", "title", "content", "visibility", "status")
        ));
        register(new CrudResourceDefinition(
            "trip-plans",
            "tm_trip_plan",
            "id",
            List.of("title", "destination_city", "summary"),
            List.of("user_id", "destination_city", "status"),
            List.of("user_id", "title", "destination_city", "start_date", "end_date", "travel_days", "budget",
                "total_cost", "status", "summary", "raw_plan_json")
        ));
        register(new CrudResourceDefinition(
            "ai-records",
            "tm_ai_analysis_record",
            "id",
            List.of("analysis_type", "target_type", "request_summary", "result_summary"),
            List.of("user_id", "analysis_type", "target_type", "target_id", "status"),
            List.of("user_id", "analysis_type", "target_type", "target_id", "request_summary", "result_summary",
                "result_json", "status")
        ));
    }

    public CrudResourceDefinition get(String key) {
        CrudResourceDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown CRUD resource: " + key);
        }
        return definition;
    }

    public List<String> keys() {
        return List.copyOf(definitions.keySet());
    }

    private void register(CrudResourceDefinition definition) {
        definitions.put(definition.key(), definition);
    }
}
