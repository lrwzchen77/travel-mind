package com.zkry.resources.service;

import java.util.List;

public record CrudResourceDefinition(
    String key,
    String tableName,
    String idColumn,
    List<String> searchColumns,
    List<String> filterColumns,
    List<String> writableColumns
) {

    public boolean allowsFilter(String column) {
        return filterColumns.contains(column);
    }

    public boolean allowsWrite(String column) {
        return writableColumns.contains(column);
    }
}
