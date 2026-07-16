package com.zkry.resources.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.common.json.utils.JsonUtils;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ChatGPT 式界面的最小持久化：会话、消息各一张表，不混入 AI 审计记录。 */
@Service
public class AssistantConversationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssistantConversationService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> conversations(long userId) {
        return jdbcTemplate.queryForList("""
                SELECT id, title, scene, trip_plan_id, create_time, update_time
                FROM tm_ai_conversation WHERE user_id = :userId AND deleted = 0
                ORDER BY update_time DESC, id DESC LIMIT 30
                """, Map.of("userId", userId));
    }

    public Map<String, Object> conversation(long userId, long conversationId) {
        return jdbcTemplate.queryForList("""
                SELECT id, title, scene, trip_plan_id, create_time, update_time
                FROM tm_ai_conversation WHERE id = :id AND user_id = :userId AND deleted = 0 LIMIT 1
                """, Map.of("id", conversationId, "userId", userId)).stream().findFirst()
            .orElseThrow(() -> new BizException("对话不存在或无权查看。"));
    }

    public List<Map<String, Object>> messages(long userId, long conversationId, int limit) {
        conversation(userId, conversationId);
        List<Map<String, Object>> messages = jdbcTemplate.queryForList("""
                SELECT id, role, content, metadata_json, create_time
                FROM tm_ai_message WHERE conversation_id = :conversationId
                ORDER BY id DESC LIMIT :limit
                """, new MapSqlParameterSource().addValue("conversationId", conversationId).addValue("limit", Math.min(Math.max(limit, 1), 20)))
            ;
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public long ensure(long userId, Long conversationId, String firstMessage, Long tripPlanId) {
        if (conversationId != null && conversationId > 0) {
            conversation(userId, conversationId);
            return conversationId;
        }
        long id = nextId();
        String title = firstMessage == null || firstMessage.isBlank() ? "新的旅行对话" : firstMessage.trim();
        if (title.length() > 32) title = title.substring(0, 32) + "…";
        jdbcTemplate.update("""
                INSERT INTO tm_ai_conversation (id, user_id, title, scene, trip_plan_id)
                VALUES (:id, :userId, :title, :scene, :tripPlanId)
                """, new MapSqlParameterSource().addValue("id", id).addValue("userId", userId).addValue("title", title)
            .addValue("scene", tripPlanId == null ? "explore" : "trip").addValue("tripPlanId", tripPlanId));
        return id;
    }

    @Transactional
    public void append(long conversationId, String role, String content, Map<String, Object> metadata) {
        if (!List.of("user", "assistant").contains(role)) throw new BizException("不支持的消息角色。");
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty() || safeContent.length() > 8000) throw new BizException("消息内容不合法。");
        jdbcTemplate.update("""
                INSERT INTO tm_ai_message (id, conversation_id, role, content, metadata_json)
                VALUES (:id, :conversationId, :role, :content, CAST(:metadata AS JSON))
                """, Map.of("id", nextId(), "conversationId", conversationId, "role", role, "content", safeContent,
            "metadata", JsonUtils.toJsonString(metadata == null ? Map.of() : metadata)));
        jdbcTemplate.update("UPDATE tm_ai_conversation SET update_time = CURRENT_TIMESTAMP WHERE id = :id", Map.of("id", conversationId));
    }

    private long nextId() { return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000); }
}
