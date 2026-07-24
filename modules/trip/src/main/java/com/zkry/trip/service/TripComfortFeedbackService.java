package com.zkry.trip.service;

import com.zkry.common.core.exception.BizException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripComfortFeedbackService {

    private static final Set<String> LABELS = Set.of("relaxed", "balanced", "intense");
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TripComfortFeedbackService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> latest(long userId, long tripId) {
        ensureTripOwner(userId, tripId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT actual_label, note, update_time
                FROM tm_trip_comfort_feedback
                WHERE trip_plan_id = :tripId AND user_id = :userId
                LIMIT 1
                """, Map.of("tripId", tripId, "userId", userId));
        if (rows.isEmpty()) return Map.of("submitted", false);
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("submitted", true);
        return result;
    }

    @Transactional
    public Map<String, Object> save(long userId, long tripId, Map<String, Object> payload) {
        Map<String, Object> trip = ensureTripOwner(userId, tripId);
        LocalDate endDate = date(trip.get("end_date"));
        if (endDate != null && endDate.isAfter(LocalDate.now())) {
            throw new BizException("行程结束后才能反馈实际体验。");
        }
        String label = text(payload, "actual_label", 16);
        if (!LABELS.contains(label)) throw new BizException("舒适度反馈标签不支持。");
        List<Map<String, Object>> predictions = jdbcTemplate.queryForList("""
                SELECT result_json
                FROM tm_ai_analysis_record
                WHERE analysis_type = 'trip_evaluate'
                  AND target_type = 'trip_plan'
                  AND target_id = :tripId
                  AND status = 'success'
                  AND deleted = 0
                ORDER BY create_time DESC, id DESC
                LIMIT 1
                """, Map.of("tripId", tripId));
        if (predictions.isEmpty() || predictions.get(0).get("result_json") == null) {
            throw new BizException("该行程尚无可反馈的舒适度预测。");
        }
        jdbcTemplate.update("""
                INSERT INTO tm_trip_comfort_feedback
                  (id, trip_plan_id, user_id, actual_label, note, prediction_json)
                VALUES
                  (:id, :tripId, :userId, :label, :note, :predictionJson)
                ON DUPLICATE KEY UPDATE
                  actual_label = VALUES(actual_label),
                  note = VALUES(note),
                  prediction_json = VALUES(prediction_json),
                  update_time = CURRENT_TIMESTAMP
                """, new MapSqlParameterSource()
            .addValue("id", nextId())
            .addValue("tripId", tripId)
            .addValue("userId", userId)
            .addValue("label", label)
            .addValue("note", text(payload, "note", 500))
            .addValue("predictionJson", String.valueOf(predictions.get(0).get("result_json"))));
        return latest(userId, tripId);
    }

    public Map<String, Object> stats() {
        Map<String, Long> labels = new LinkedHashMap<>();
        LABELS.stream().sorted().forEach(label -> labels.put(label, 0L));
        jdbcTemplate.queryForList("""
                SELECT actual_label, COUNT(*) AS count
                FROM tm_trip_comfort_feedback
                GROUP BY actual_label
                """, Map.of()).forEach(row -> {
                    String label = String.valueOf(row.get("actual_label"));
                    if (LABELS.contains(label)) labels.put(label, ((Number) row.get("count")).longValue());
                });
        long total = labels.values().stream().mapToLong(Long::longValue).sum();
        return Map.of("total", total, "labels", labels);
    }

    private Map<String, Object> ensureTripOwner(long userId, long tripId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, end_date FROM tm_trip_plan
                WHERE id = :tripId AND user_id = :userId AND deleted = 0
                LIMIT 1
                """, Map.of("tripId", tripId, "userId", userId));
        if (rows.isEmpty()) throw new BizException("行程不存在或无权操作。");
        return rows.get(0);
    }

    private String text(Map<String, Object> payload, String key, int maxLength) {
        Object value = payload == null ? null : payload.get(key);
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.length() > maxLength) throw new BizException("反馈内容过长。");
        return text;
    }

    private long nextId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }

    private LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate();
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
