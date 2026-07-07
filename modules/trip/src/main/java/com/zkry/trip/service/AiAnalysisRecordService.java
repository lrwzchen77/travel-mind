package com.zkry.trip.service;

import com.zkry.common.json.utils.JsonUtils;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAnalysisRecordService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AiAnalysisRecordService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long save(
        Long userId,
        String analysisType,
        String targetType,
        Long targetId,
        String requestSummary,
        PythonAiCallResult<?> result
    ) {
        long id = nextId();
        String status = result != null && result.success() ? "success" : "failed";
        String message = result == null ? "Python AI service returned empty result" : result.message();
        String resultSummary = summarizeResult(result);
        String resultJson = result != null && result.rawJson() != null && !result.rawJson().isBlank()
            ? result.rawJson()
            : JsonUtils.toJsonString(Map.of("success", "success".equals(status), "message", safe(message)));
        jdbcTemplate.update("""
                INSERT INTO tm_ai_analysis_record
                  (id, user_id, analysis_type, target_type, target_id, request_summary, result_summary, result_json, status)
                VALUES
                  (:id, :userId, :analysisType, :targetType, :targetId, :requestSummary, :resultSummary, :resultJson, :status)
                """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("analysisType", analysisType)
                .addValue("targetType", targetType)
                .addValue("targetId", targetId)
                .addValue("requestSummary", clip(requestSummary))
                .addValue("resultSummary", clip(resultSummary))
                .addValue("resultJson", resultJson)
                .addValue("status", status));
        return id;
    }

    public Map<String, Object> latest(String analysisType, String targetType, Long targetId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM tm_ai_analysis_record
                WHERE analysis_type = :analysisType
                  AND target_type = :targetType
                  AND target_id = :targetId
                  AND deleted = 0
                ORDER BY create_time DESC, id DESC
                LIMIT 1
                """,
            Map.of("analysisType", analysisType, "targetType", targetType, "targetId", targetId));
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private String summarizeResult(PythonAiCallResult<?> result) {
        if (result == null) {
            return "empty result";
        }
        if (!result.success()) {
            return safe(result.message());
        }
        Object data = result.data();
        if (data == null) {
            return safe(result.message());
        }
        return JsonUtils.toJsonString(data);
    }

    private String clip(String value) {
        String safe = safe(value);
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long nextId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
