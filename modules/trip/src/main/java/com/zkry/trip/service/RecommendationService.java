package com.zkry.trip.service;

import com.zkry.trip.dto.ai.DestinationItemInput;
import com.zkry.trip.dto.ai.DestinationsIndexRequest;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.RecommendRequest;
import com.zkry.trip.dto.ai.RecommendResponse;
import com.zkry.trip.dto.ai.RecommendResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final PythonAiClient pythonAiClient;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RecommendationService(PythonAiClient pythonAiClient, NamedParameterJdbcTemplate jdbcTemplate) {
        this.pythonAiClient = pythonAiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RecommendResult> recommendForUser(long userId, String type, String cityFilter, int limit) {
        Map<String, Object> preference = loadPreference(userId);
        RecommendRequest request = buildRequest(preference, type, cityFilter, limit);
        PythonAiCallResult<RecommendResponse> result = pythonAiClient.recommend(request);
        if (!result.success() || result.data() == null) {
            return fallbackRecommendations(type, cityFilter, limit);
        }
        List<RecommendResult> items = result.data().results();
        if (items == null || items.isEmpty()) {
            return fallbackRecommendations(type, cityFilter, limit);
        }
        // Log recommendations
        for (RecommendResult item : items) {
            logRecommendation(userId, item.itemType(), item.itemId(), item.score());
        }
        return items;
    }

    public int reindexDestinations() {
        List<DestinationItemInput> cities = jdbcTemplate.queryForList(
            "SELECT id, name, province, description, popularity FROM tm_city WHERE deleted = 0 AND status = 1",
            Map.of()
        ).stream().map(row -> new DestinationItemInput(
            ((Number) row.get("id")).intValue(),
            "city",
            safe(row.get("name")),
            safe(row.get("province")),
            safe(row.get("description")),
            Collections.emptyList(),
            0.0,
            toInt(row.get("popularity"))
        )).collect(Collectors.toList());

        List<DestinationItemInput> pois = Collections.emptyList();
        try {
            pois = jdbcTemplate.queryForList(
                "SELECT id, city, name, kind, address, rating, tags, popularity FROM tm_map_poi WHERE deleted = 0 AND status = 1",
                Map.of()
            ).stream().map(row -> new DestinationItemInput(
                ((Number) row.get("id")).intValue(),
                safe(row.get("kind")),
                safe(row.get("name")),
                safe(row.get("city")),
                safe(row.get("address")),
                parseTags(row.get("tags")),
                toDouble(row.get("rating")),
                toInt(row.get("popularity"))
            )).collect(Collectors.toList());
        } catch (Exception ignored) {
            // tm_map_poi may not exist yet
        }

        List<DestinationItemInput> all = new java.util.ArrayList<>();
        all.addAll(cities);
        all.addAll(pois);

        if (all.isEmpty()) {
            return 0;
        }

        // Batch in chunks of 200 to avoid large payloads
        int totalIndexed = 0;
        int batchSize = 200;
        for (int i = 0; i < all.size(); i += batchSize) {
            List<DestinationItemInput> batch = all.subList(i, Math.min(i + batchSize, all.size()));
            PythonAiCallResult<Object> result = pythonAiClient.indexDestinations(new DestinationsIndexRequest(batch));
            if (result.success()) {
                totalIndexed += batch.size();
            }
        }
        return totalIndexed;
    }

    public void recordFeedback(long userId, long targetId, String targetType, String feedback) {
        jdbcTemplate.update(
            "INSERT INTO tm_recommendation_log (id, user_id, recommend_type, target_id, target_type, score, user_feedback) "
                + "VALUES (:id, :userId, :type, :targetId, :targetType, 0, :feedback)",
            Map.of(
                "id", nextId(),
                "userId", userId,
                "type", targetType,
                "targetId", targetId,
                "targetType", targetType,
                "feedback", feedback
            )
        );
    }

    private Map<String, Object> loadPreference(long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_user_preference WHERE user_id = :userId AND deleted = 0 ORDER BY update_time DESC LIMIT 1",
            Map.of("userId", userId)
        );
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private RecommendRequest buildRequest(Map<String, Object> preference, String type, String cityFilter, int limit) {
        String budgetLevel = safe(preference.get("budget_level"));
        String travelStyle = safe(preference.get("travel_style"));
        String preferredCity = safe(preference.get("preferred_city"));
        String preferredTagsStr = safe(preference.get("preferred_tags"));
        List<String> preferredTags = preferredTagsStr.isBlank()
            ? Collections.emptyList()
            : List.of(preferredTagsStr.split(","));
        String transportation = safe(preference.get("transportation"));
        String hotelLevel = safe(preference.get("hotel_level"));
        String dietPreference = safe(preference.get("diet_preference"));
        return new RecommendRequest(
            budgetLevel.isBlank() ? null : budgetLevel,
            travelStyle.isBlank() ? null : travelStyle,
            preferredCity.isBlank() ? null : preferredCity,
            preferredTags,
            transportation.isBlank() ? null : transportation,
            hotelLevel.isBlank() ? null : hotelLevel,
            dietPreference.isBlank() ? null : dietPreference,
            type,
            cityFilter,
            Math.min(Math.max(limit, 1), 50),
            Collections.emptyList()
        );
    }

    private List<RecommendResult> fallbackRecommendations(String type, String cityFilter, int limit) {
        String table = resolveTable(type);
        if (table == null) {
            return Collections.emptyList();
        }
        boolean isCityTable = "tm_city".equals(table);
        String sql;
        if (isCityTable) {
            sql = "SELECT id, name, province, description, popularity FROM " + table
                + " WHERE deleted = 0 AND status = 1"
                + " ORDER BY popularity DESC LIMIT :limit";
        } else {
            sql = "SELECT id, name, city, address, rating, popularity, tags FROM " + table
                + " WHERE deleted = 0 AND status = 1"
                + (cityFilter != null && !cityFilter.isBlank() ? " AND city = :cityFilter" : "")
                + " ORDER BY popularity DESC, rating DESC LIMIT :limit";
        }
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("limit", limit);
        if (cityFilter != null && !cityFilter.isBlank() && !isCityTable) {
            params.put("cityFilter", cityFilter);
        }
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql, params);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> new RecommendResult(
            ((Number) row.get("id")).intValue(),
            type,
            safe(row.get("name")),
            safe(isCityTable ? row.get("name") : row.get("city")),
            safe(isCityTable ? row.get("description") : row.get("address")),
            isCityTable ? Collections.emptyList() : parseTags(row.get("tags")),
            isCityTable ? 0.0 : toDouble(row.get("rating")),
            toInt(row.get("popularity")),
            0.5,
            "热门推荐"
        )).collect(Collectors.toList());
    }

    private String resolveTable(String type) {
        return switch (type) {
            case "city" -> "tm_city";
            case "attraction", "hotel", "restaurant" -> "tm_map_poi";
            default -> null;
        };
    }

    private void logRecommendation(long userId, String recommendType, int targetId, double score) {
        try {
            jdbcTemplate.update(
                "INSERT INTO tm_recommendation_log (id, user_id, recommend_type, target_id, target_type, score) "
                    + "VALUES (:id, :userId, :type, :targetId, :targetType, :score)",
                Map.of(
                    "id", nextId(),
                    "userId", userId,
                    "type", recommendType,
                    "targetId", targetId,
                    "targetType", recommendType,
                    "score", score
                )
            );
        } catch (Exception ignored) {
            // Logging failure should not break recommendations
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> parseTags(Object value) {
        String str = safe(value);
        if (str.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(str.split(","));
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private long nextId() {
        return java.util.concurrent.ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
