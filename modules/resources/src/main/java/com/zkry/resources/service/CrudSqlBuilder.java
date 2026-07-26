package com.zkry.resources.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CrudSqlBuilder {

    public QuerySpec buildListQuery(CrudResourceDefinition definition, ResourceSearchCriteria criteria) {
        WhereSpec where = buildWhere(definition, criteria);
        Map<String, Object> params = new LinkedHashMap<>(where.params());
        params.put("limit", criteria.normalizedPageSize());
        params.put("offset", criteria.offset());
        String sql = "SELECT * FROM " + definition.tableName() + where.sql()
            + " ORDER BY update_time DESC, " + definition.idColumn() + " DESC LIMIT :limit OFFSET :offset";
        return new QuerySpec(sql, params);
    }

    public QuerySpec buildCountQuery(CrudResourceDefinition definition, ResourceSearchCriteria criteria) {
        WhereSpec where = buildWhere(definition, criteria);
        return new QuerySpec("SELECT COUNT(1) FROM " + definition.tableName() + where.sql(), where.params());
    }

    private WhereSpec buildWhere(CrudResourceDefinition definition, ResourceSearchCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        clauses.add("deleted = 0");
        if (definition.scoped()) {
            clauses.add(definition.scopeColumn() + " = :resourceScope");
            params.put("resourceScope", definition.scopeValue());
        }

        if (criteria.keyword() != null && !definition.searchColumns().isEmpty()) {
            List<String> keywordClauses = definition.searchColumns().stream()
                .map(column -> column + " LIKE :keyword")
                .toList();
            clauses.add("(" + String.join(" OR ", keywordClauses) + ")");
            params.put("keyword", "%" + criteria.keyword() + "%");
        }
        if (criteria.cityId() != null && definition.allowsFilter("city_id")) {
            clauses.add("city_id = :cityId");
            params.put("cityId", criteria.cityId());
        }
        if (criteria.category() != null && definition.allowsFilter("category")) {
            clauses.add("category = :category");
            params.put("category", criteria.category());
        }
        if (criteria.tag() != null && definition.allowsFilter("tags")) {
            clauses.add("tags LIKE :tag");
            params.put("tag", "%" + criteria.tag() + "%");
        }
        if (criteria.ratingMin() != null && definition.allowsFilter("rating")) {
            clauses.add("rating >= :ratingMin");
            params.put("ratingMin", criteria.ratingMin());
        }
        if (criteria.ratingMax() != null && definition.allowsFilter("rating")) {
            clauses.add("rating <= :ratingMax");
            params.put("ratingMax", criteria.ratingMax());
        }
        criteria.exactFilters().forEach((column, value) -> {
            if (definition.allowsFilter(column)) {
                String parameter = toParameterName(column);
                clauses.add(column + " = :" + parameter);
                params.put(parameter, value);
            }
        });

        return new WhereSpec(" WHERE " + String.join(" AND ", clauses), params);
    }

    private String toParameterName(String column) {
        StringBuilder parameter = new StringBuilder();
        boolean uppercaseNext = false;
        for (char ch : column.toCharArray()) {
            if (ch == '_') {
                uppercaseNext = true;
            } else if (uppercaseNext) {
                parameter.append(Character.toUpperCase(ch));
                uppercaseNext = false;
            } else {
                parameter.append(ch);
            }
        }
        return parameter.toString();
    }

    public record QuerySpec(String sql, Map<String, Object> params) {
    }

    private record WhereSpec(String sql, Map<String, Object> params) {
    }
}
