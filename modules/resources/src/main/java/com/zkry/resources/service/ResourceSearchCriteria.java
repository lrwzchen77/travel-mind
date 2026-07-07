package com.zkry.resources.service;

import com.zkry.common.core.domain.PageQuery;
import java.util.LinkedHashMap;
import java.util.Map;

public record ResourceSearchCriteria(
    String keyword,
    Long cityId,
    String category,
    String tag,
    Double ratingMin,
    Double ratingMax,
    Long userId,
    Long attractionId,
    Long targetId,
    String targetType,
    String analysisType,
    String status,
    int pageNum,
    int pageSize
) {

    public static ResourceSearchCriteria of(
        String keyword,
        Long cityId,
        String category,
        String tag,
        Double ratingMin,
        Double ratingMax,
        Integer pageNum,
        Integer pageSize
    ) {
        return of(keyword, cityId, category, tag, ratingMin, ratingMax, null, null, null, null, null, null, pageNum,
            pageSize);
    }

    public static ResourceSearchCriteria of(
        String keyword,
        Long cityId,
        String category,
        String tag,
        Double ratingMin,
        Double ratingMax,
        Long userId,
        Long attractionId,
        Long targetId,
        String targetType,
        String analysisType,
        String status,
        Integer pageNum,
        Integer pageSize
    ) {
        return new ResourceSearchCriteria(
            blankToNull(keyword),
            cityId,
            blankToNull(category),
            blankToNull(tag),
            ratingMin,
            ratingMax,
            userId,
            attractionId,
            targetId,
            blankToNull(targetType),
            blankToNull(analysisType),
            blankToNull(status),
            pageNum == null ? PageQuery.DEFAULT_PAGE_NUM : pageNum,
            pageSize == null ? PageQuery.DEFAULT_PAGE_SIZE : pageSize
        );
    }

    public Map<String, Object> exactFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        putIfPresent(filters, "user_id", userId);
        putIfPresent(filters, "attraction_id", attractionId);
        putIfPresent(filters, "target_id", targetId);
        putIfPresent(filters, "target_type", targetType);
        putIfPresent(filters, "analysis_type", analysisType);
        putIfPresent(filters, "status", status);
        return filters;
    }

    public int normalizedPageNum() {
        return Math.max(pageNum, PageQuery.DEFAULT_PAGE_NUM);
    }

    public int normalizedPageSize() {
        if (pageSize < 1) {
            return PageQuery.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, PageQuery.MAX_PAGE_SIZE);
    }

    public long offset() {
        return (long) (normalizedPageNum() - 1) * normalizedPageSize();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void putIfPresent(Map<String, Object> filters, String column, Object value) {
        if (value != null) {
            filters.put(column, value);
        }
    }
}
