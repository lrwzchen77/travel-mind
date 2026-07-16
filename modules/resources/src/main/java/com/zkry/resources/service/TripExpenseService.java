package com.zkry.resources.service;

import com.zkry.common.core.exception.BizException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 行程实际花费；始终通过行程归属校验，不接受前端传入 userId。 */
@Service
public class TripExpenseService {

    private static final List<String> CATEGORIES = List.of("transport", "stay", "food", "ticket", "shopping", "other");
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TripExpenseService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> summary(long userId, long tripId) {
        BigDecimal budget = tripBudget(userId, tripId);
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                SELECT id, category, title, amount, spent_on, note, create_time
                FROM tm_trip_expense WHERE trip_plan_id = :tripId AND deleted = 0
                ORDER BY spent_on DESC, id DESC
                """, Map.of("tripId", tripId));
        BigDecimal actual = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM tm_trip_expense WHERE trip_plan_id = :tripId AND deleted = 0",
            Map.of("tripId", tripId), BigDecimal.class);
        actual = actual == null ? BigDecimal.ZERO : actual;
        return Map.of("budget", budget, "actual", actual, "remaining", budget.subtract(actual), "items", items);
    }

    @Transactional
    public Map<String, Object> create(long userId, long tripId, Map<String, Object> payload) {
        tripBudget(userId, tripId);
        String category = text(payload, "category", 32);
        if (!CATEGORIES.contains(category)) throw new BizException("支出分类不支持。");
        String title = text(payload, "title", 128);
        if (title.isBlank()) throw new BizException("请填写这笔花费用在哪里。");
        BigDecimal amount = amount(payload == null ? null : payload.get("amount"));
        LocalDate spentOn = date(text(payload, "spent_on", 32));
        jdbcTemplate.update("""
                INSERT INTO tm_trip_expense (id, trip_plan_id, category, title, amount, spent_on, note)
                VALUES (:id, :tripId, :category, :title, :amount, :spentOn, :note)
                """, new MapSqlParameterSource()
            .addValue("id", nextId()).addValue("tripId", tripId).addValue("category", category).addValue("title", title)
            .addValue("amount", amount).addValue("spentOn", spentOn).addValue("note", text(payload, "note", 500)));
        return summary(userId, tripId);
    }

    @Transactional
    public void delete(long userId, long tripId, long expenseId) {
        tripBudget(userId, tripId);
        int changed = jdbcTemplate.update("""
                UPDATE tm_trip_expense SET deleted = 1
                WHERE id = :expenseId AND trip_plan_id = :tripId AND deleted = 0
                """, Map.of("expenseId", expenseId, "tripId", tripId));
        if (changed == 0) throw new BizException("这笔花费不存在或已删除。");
    }

    private BigDecimal tripBudget(long userId, long tripId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT budget FROM tm_trip_plan WHERE id = :tripId AND user_id = :userId AND deleted = 0 LIMIT 1",
            Map.of("tripId", tripId, "userId", userId));
        if (rows.isEmpty()) throw new BizException("行程不存在或无权操作。");
        Object value = rows.get(0).get("budget");
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value == null ? 0 : value));
    }

    private BigDecimal amount(Object value) {
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value));
            if (amount.stripTrailingZeros().scale() > 2) throw new BizException("金额最多保留两位小数。");
            amount = amount.setScale(2);
            if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("1000000")) > 0) throw new BizException("金额需大于 0 且不超过 100 万。");
            return amount;
        } catch (NumberFormatException ex) {
            throw new BizException("金额格式不正确。");
        }
    }

    private LocalDate date(String value) {
        if (value.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BizException("消费日期格式不正确。");
        }
    }

    private String text(Map<String, Object> payload, String key, int max) {
        Object value = payload == null ? null : payload.get(key);
        String result = value == null ? "" : String.valueOf(value).trim();
        if (result.length() > max) throw new BizException(key + " 内容过长。");
        return result;
    }

    private long nextId() { return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000); }
}
