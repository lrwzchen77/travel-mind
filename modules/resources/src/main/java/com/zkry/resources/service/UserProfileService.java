package com.zkry.resources.service;

import java.util.List;
import java.util.Map;
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
