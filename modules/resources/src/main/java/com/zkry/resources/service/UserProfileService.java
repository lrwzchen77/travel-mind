package com.zkry.resources.service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final CrudResourceService crudResourceService;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserProfileService(CrudResourceService crudResourceService, NamedParameterJdbcTemplate jdbcTemplate) {
        this.crudResourceService = crudResourceService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> profile(long userId) {
        Map<String, Object> user = crudResourceService.detail("users", userId);
        List<Map<String, Object>> preferences = jdbcTemplate.queryForList(
            "SELECT * FROM tm_user_preference WHERE user_id = :userId AND deleted = 0 ORDER BY update_time DESC LIMIT 1",
            Map.of("userId", userId));
        return Map.of("user", user, "preference", preferences.isEmpty() ? Map.of() : preferences.get(0));
    }

    @Transactional
    public Map<String, Object> updateProfile(long userId, Map<String, Object> payload) {
        Object user = payload == null ? null : payload.get("user");
        Object preference = payload == null ? null : payload.get("preference");
        if (user instanceof Map<?, ?> userMap) {
            crudResourceService.update("users", userId, castMap(userMap));
        }
        if (preference instanceof Map<?, ?> preferenceMap) {
            upsertPreference(userId, castMap(preferenceMap));
        }
        return profile(userId);
    }

    public Map<String, Object> exportData(long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", profile(userId));
        data.put("favorites", rows("SELECT * FROM tm_favorite WHERE user_id = :userId AND deleted = 0", userId));
        data.put("travel_notes", rows("SELECT * FROM tm_travel_note WHERE user_id = :userId AND deleted = 0", userId));
        data.put("travel_note_likes", rows("SELECT * FROM tm_travel_note_like WHERE user_id = :userId", userId));
        data.put("travel_note_comments", rows("SELECT * FROM tm_travel_note_comment WHERE user_id = :userId AND deleted = 0", userId));
        data.put("inspiration_items", rows("SELECT * FROM tm_inspiration_item WHERE user_id = :userId AND deleted = 0", userId));
        data.put("trip_plans", rows("SELECT * FROM tm_trip_plan WHERE user_id = :userId AND deleted = 0", userId));
        data.put("trip_expenses", rows("""
            SELECT e.* FROM tm_trip_expense e JOIN tm_trip_plan p ON p.id = e.trip_plan_id
            WHERE p.user_id = :userId AND p.deleted = 0 AND e.deleted = 0
            """, userId));
        data.put("trip_comfort_feedback", rows("SELECT * FROM tm_trip_comfort_feedback WHERE user_id = :userId", userId));
        data.put("ai_analysis_records", rows("SELECT * FROM tm_ai_analysis_record WHERE user_id = :userId AND deleted = 0", userId));
        data.put("conversations", rows("SELECT * FROM tm_ai_conversation WHERE user_id = :userId AND deleted = 0", userId));
        data.put("messages", rows("""
            SELECT m.* FROM tm_ai_message m
            JOIN tm_ai_conversation c ON c.id = m.conversation_id
            WHERE c.user_id = :userId AND c.deleted = 0
            """, userId));
        data.put("memories", rows("SELECT * FROM tm_trip_memory WHERE user_id = :userId", userId));
        data.put("memory_items", rows("""
            SELECT i.* FROM tm_trip_memory_item i JOIN tm_trip_memory m ON m.id = i.memory_id
            WHERE m.user_id = :userId
            """, userId));
        data.put("notifications", rows("SELECT * FROM tm_notification WHERE user_id = :userId", userId));
        data.put("exported_at", java.time.Instant.now().toString());
        return data;
    }

    private List<Map<String, Object>> rows(String sql, long userId) {
        return jdbcTemplate.queryForList(sql, Map.of("userId", userId));
    }

    private void upsertPreference(long userId, Map<String, Object> values) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM tm_user_preference WHERE user_id = :userId AND deleted = 0 ORDER BY update_time DESC LIMIT 1",
            Map.of("userId", userId));
        values.put("user_id", userId);
        if (rows.isEmpty()) {
            crudResourceService.create("user-preferences", values);
        } else {
            crudResourceService.update("user-preferences", ((Number) rows.get(0).get("id")).longValue(), values);
        }
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> map.put(String.valueOf(key), value));
        return map;
    }
}
