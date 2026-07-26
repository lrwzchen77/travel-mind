package com.zkry.resources.service;

import com.zkry.common.core.domain.PageResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            + " WHERE " + definition.idColumn() + " = :id AND deleted = 0" + scopeSql(definition);
        params = scopedParams(definition, params);
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
        boolean poi = "tm_map_poi".equals(definition.tableName());
        long id = nextId();
        preparePoi(definition, values, true);
        if (!poi) values.put(definition.idColumn(), id);

        String columns = String.join(", ", values.keySet());
        String placeholders = ":" + String.join(", :", values.keySet());
        String sql = "INSERT INTO " + definition.tableName() + " (" + columns + ") VALUES (" + placeholders + ")";
        if (poi) {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(sql, new MapSqlParameterSource(values), keyHolder, new String[]{definition.idColumn()});
            id = keyHolder.getKey().longValue();
        } else {
            jdbcTemplate.update(sql, values);
        }
        return detail(resourceKey, id);
    }

    @Transactional
    public Map<String, Object> update(String resourceKey, long id, Map<String, Object> payload) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        Map<String, Object> values = writablePayload(definition, payload);
        preparePoi(definition, values, false);
        if (values.isEmpty()) {
            return detail(resourceKey, id);
        }
        MapSqlParameterSource params = new MapSqlParameterSource(values).addValue("id", id);
        String assignments = String.join(", ", values.keySet().stream().map(column -> column + " = :" + column).toList());
        int updated = jdbcTemplate.update("UPDATE " + definition.tableName() + " SET " + assignments
            + " WHERE " + definition.idColumn() + " = :id AND deleted = 0" + scopeSql(definition),
            scopedParams(definition, params));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return detail(resourceKey, id);
    }

    @Transactional
    public void delete(String resourceKey, long id) {
        CrudResourceDefinition definition = registry.get(resourceKey);
        int updated = jdbcTemplate.update("UPDATE " + definition.tableName()
            + " SET deleted = 1 WHERE " + definition.idColumn() + " = :id AND deleted = 0" + scopeSql(definition),
            scopedParams(definition, Map.of("id", id)));
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
            + " SET status = :status WHERE " + definition.idColumn() + " = :id AND deleted = 0" + scopeSql(definition),
            scopedParams(definition, Map.of("status", status, "id", id)));
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

    private void preparePoi(CrudResourceDefinition definition, Map<String, Object> values, boolean creating) {
        if (!"tm_map_poi".equals(definition.tableName())) return;
        if (definition.scoped()) values.put(definition.scopeColumn(), definition.scopeValue());
        if (creating) {
            values.put("source", "manual");
            values.put("source_id", "manual-" + UUID.randomUUID());
            values.put("source_updated_at", Instant.now().toString());
        }
        Object cityId = values.get("city_id");
        Object city = values.get("city");
        if ((city == null || city.toString().isBlank()) && cityId != null && !cityId.toString().isBlank()) {
            values.put("city", cityName(cityId));
        } else if ((cityId == null || cityId.toString().isBlank()) && city != null && !city.toString().isBlank()) {
            Long resolved = jdbcTemplate.query("SELECT id FROM tm_city WHERE name = :city AND deleted = 0 LIMIT 1",
                Map.of("city", city.toString().trim()), rs -> rs.next() ? rs.getLong(1) : null);
            if (resolved != null) values.put("city_id", resolved);
        }
        validatePoi(values, creating);
    }

    private String cityName(Object cityId) {
        String name = jdbcTemplate.query("SELECT name FROM tm_city WHERE id = :id AND deleted = 0 LIMIT 1",
            Map.of("id", cityId), rs -> rs.next() ? rs.getString(1) : null);
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "城市不存在");
        return name;
    }

    private void validatePoi(Map<String, Object> values, boolean required) {
        if (required && (blank(values.get("name")) || blank(values.get("city")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "地点名称和城市不能为空");
        }
        if ((!required && values.containsKey("name") && blank(values.get("name")))
            || (!required && values.containsKey("city") && blank(values.get("city")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "地点名称和城市不能为空");
        }
        if (required || values.containsKey("longitude")) {
            validCoordinate(values.get("longitude"), "经度", -180, 180);
        }
        if (required || values.containsKey("latitude")) {
            validCoordinate(values.get("latitude"), "纬度", -90, 90);
        }
        Object kind = values.get("kind");
        if ((required || kind != null) && (kind == null
            || !Set.of("attraction", "hotel", "restaurant").contains(kind.toString()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "地点大类必须是 attraction、hotel 或 restaurant");
        }
    }

    private void validCoordinate(Object value, String label, double minimum, double maximum) {
        try {
            double coordinate = Double.parseDouble(String.valueOf(value));
            if (!Double.isFinite(coordinate) || coordinate < minimum || coordinate > maximum) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "格式不正确");
        }
    }

    private boolean blank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private String scopeSql(CrudResourceDefinition definition) {
        return definition.scoped() ? " AND " + definition.scopeColumn() + " = :resourceScope" : "";
    }

    private Map<String, Object> scopedParams(CrudResourceDefinition definition, Map<String, Object> params) {
        if (!definition.scoped()) return params;
        Map<String, Object> values = new LinkedHashMap<>(params);
        values.put("resourceScope", definition.scopeValue());
        return values;
    }

    private MapSqlParameterSource scopedParams(
        CrudResourceDefinition definition, MapSqlParameterSource params
    ) {
        if (definition.scoped()) params.addValue("resourceScope", definition.scopeValue());
        return params;
    }

    private long nextId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
