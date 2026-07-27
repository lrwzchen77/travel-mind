package com.zkry.resources.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NotificationService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(long userId) {
        return jdbcTemplate.queryForList("""
            SELECT id, type, title, content, target_url, read_at, create_time
            FROM tm_notification WHERE user_id = :userId
            ORDER BY create_time DESC, id DESC LIMIT 50
            """, Map.of("userId", userId));
    }

    @Transactional
    public void notify(long userId, String type, String title, String content, String targetUrl) {
        jdbcTemplate.update("""
            INSERT INTO tm_notification (id, user_id, type, title, content, target_url)
            VALUES (:id, :userId, :type, :title, :content, :targetUrl)
            """, Map.of("id", System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000),
                "userId", userId, "type", type, "title", title, "content", content, "targetUrl", targetUrl));
    }

    @Transactional
    public void read(long userId, long id) {
        jdbcTemplate.update("""
            UPDATE tm_notification SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
            WHERE id = :id AND user_id = :userId
            """, Map.of("id", id, "userId", userId));
    }

    @Transactional
    public void readAll(long userId) {
        jdbcTemplate.update("UPDATE tm_notification SET read_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND read_at IS NULL",
            Map.of("userId", userId));
    }
}
