package com.zkry.resources.service;

import java.util.List;

public record CrudResourceDefinition(
    String key,
    String tableName,
    String idColumn,
    List<String> searchColumns,
    List<String> filterColumns,
    List<String> writableColumns,
    String scopeColumn,
    String scopeValue
) {

    public CrudResourceDefinition(
        String key, String tableName, String idColumn, List<String> searchColumns,
        List<String> filterColumns, List<String> writableColumns
    ) {
        this(key, tableName, idColumn, searchColumns, filterColumns, writableColumns, null, null);
    }

    public boolean allowsFilter(String column) {
        return filterColumns.contains(column);
    }

    public boolean allowsWrite(String column) {
        return writableColumns.contains(column);
    }

    public boolean scoped() {
        return scopeColumn != null && scopeValue != null;
    }
}
