package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CrudSqlBuilderTest {

    @Test
    void buildsPagedQueryWithKeywordAndFilters() {
        CrudResourceDefinition definition = new CrudResourceDefinition(
            "attractions",
            "tm_attraction",
            "id",
            java.util.List.of("name", "description"),
            java.util.List.of("city_id", "category", "tags", "rating"),
            java.util.List.of("city_id", "name", "category", "rating", "tags", "status")
        );
        ResourceSearchCriteria criteria = new ResourceSearchCriteria(
            "lake",
            2001L,
            "nature",
            "family",
            4.5,
            5.0,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            20
        );

        CrudSqlBuilder.QuerySpec spec = new CrudSqlBuilder().buildListQuery(definition, criteria);

        assertThat(spec.sql()).contains("FROM tm_attraction");
        assertThat(spec.sql()).contains("deleted = 0");
        assertThat(spec.sql()).contains("(name LIKE :keyword OR description LIKE :keyword)");
        assertThat(spec.sql()).contains("city_id = :cityId");
        assertThat(spec.sql()).contains("category = :category");
        assertThat(spec.sql()).contains("tags LIKE :tag");
        assertThat(spec.sql()).contains("rating >= :ratingMin");
        assertThat(spec.sql()).contains("rating <= :ratingMax");
        assertThat(spec.sql()).contains("LIMIT :limit OFFSET :offset");
        assertThat(spec.params()).containsAllEntriesOf(Map.of(
            "keyword", "%lake%",
            "cityId", 2001L,
            "category", "nature",
            "tag", "%family%",
            "ratingMin", 4.5,
            "ratingMax", 5.0,
            "limit", 20,
            "offset", 0L
        ));
    }

    @Test
    void buildsExactFiltersForUserAndAttractionScopedNotes() {
        CrudResourceDefinition definition = new CrudResourceDefinition(
            "travel-notes",
            "tm_travel_note",
            "id",
            java.util.List.of("title", "content"),
            java.util.List.of("user_id", "city_id", "attraction_id", "status"),
            java.util.List.of("user_id", "city_id", "attraction_id", "title", "content", "visibility", "status")
        );
        ResourceSearchCriteria criteria = ResourceSearchCriteria.of(null, null, null, null, null, null, 1001L, 3001L,
            null, null, null, "1", 1, 10);

        CrudSqlBuilder.QuerySpec spec = new CrudSqlBuilder().buildListQuery(definition, criteria);

        assertThat(spec.sql()).contains("user_id = :userId");
        assertThat(spec.sql()).contains("attraction_id = :attractionId");
        assertThat(spec.sql()).contains("status = :status");
        assertThat(spec.params()).containsAllEntriesOf(Map.of(
            "userId", 1001L,
            "attractionId", 3001L,
            "status", "1"
        ));
    }
}
