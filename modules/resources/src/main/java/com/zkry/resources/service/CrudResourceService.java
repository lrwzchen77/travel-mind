package com.zkry.resources.service;

import com.zkry.common.core.domain.PageResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class CrudResourceService {

    private final CrudResourceRegistry registry;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final CrudSqlBuilder sqlBuilder = new CrudSqlBuilder();

    public CrudResourceService(CrudResourceRegistry registry, NamedParameterJdbcTemplate jdbcTemplate) {
        this.registry = registry;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> list(String resourceKey, ResourceSearchCriteria criteria) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        CrudSqlBuilder.QuerySpec list = sqlBuilder.buildListQuery(definition, criteria);
        CrudSqlBuilder.QuerySpec count = sqlBuilder.buildCountQuery(definition, criteria);
        Long total = jdbcTemplate.queryForObject(count.sql(), count.params(), Long.class);
        return PageResult.of(jdbcTemplate.queryForList(list.sql(), list.params()), total == null ? 0 : total,
            criteria.normalizedPageNum(), criteria.normalizedPageSize());
    }

    public Map<String, Object> detail(String resourceKey, long id) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        Map<String, Object> params = Map.of("id", id);
        String sql = "SELECT * FROM " + definition.tableName()
            + " WHERE " + definition.idColumn() + " = :id AND deleted = 0";
        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
            }
            int columns = rs.getMetaData().getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columns; i++) {
                row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            return row;
        });
    }

    @Transactional
    public Map<String, Object> create(String resourceKey, Map<String, Object> payload) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        Map<String, Object> values = writablePayload(definition, payload);
        long id = nextId();
        values.put(definition.idColumn(), id);

        String columns = String.join(", ", values.keySet());
        String placeholders = ":" + String.join(", :", values.keySet());
        jdbcTemplate.update("INSERT INTO " + definition.tableName() + " (" + columns + ") VALUES (" + placeholders + ")",
            values);
        return detail(resourceKey, id);
    }

    @Transactional
    public Map<String, Object> update(String resourceKey, long id, Map<String, Object> payload) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        Map<String, Object> values = writablePayload(definition, payload);
        if (values.isEmpty()) {
            return detail(resourceKey, id);
        }
        MapSqlParameterSource params = new MapSqlParameterSource(values).addValue("id", id);
        String assignments = String.join(", ", values.keySet().stream().map(column -> column + " = :" + column).toList());
        int updated = jdbcTemplate.update("UPDATE " + definition.tableName() + " SET " + assignments
            + " WHERE " + definition.idColumn() + " = :id AND deleted = 0", params);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return detail(resourceKey, id);
    }

    @Transactional
    public void delete(String resourceKey, long id) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        int updated = jdbcTemplate.update("UPDATE " + definition.tableName()
            + " SET deleted = 1 WHERE " + definition.idColumn() + " = :id AND deleted = 0", Map.of("id", id));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    @Transactional
    public Map<String, Object> updateStatus(String resourceKey, long id, int status) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        if (!definition.allowsWrite("status")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource does not support status");
        }
        int updated = jdbcTemplate.update("UPDATE " + definition.tableName()
            + " SET status = :status WHERE " + definition.idColumn() + " = :id AND deleted = 0",
            Map.of("status", status, "id", id));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return detail(resourceKey, id);
    }

    private Map<String, Object> writablePayload(CrudResourceDefinition definition, Map<String, Object> payload) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (payload == null) {
            return values;
        }
        payload.forEach((key, value) -> {
            String column = toColumnName(key);
            if (definition.allowsWrite(column)) {
                values.put(column, value);
            }
        });
        return values;
    }

    private String toColumnName(String key) {
        StringBuilder result = new StringBuilder();
        for (char ch : key.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else if (ch == '-') {
                result.append('_');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private long nextId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
