package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CrudResourceRegistryTest {

    @Test
    void registersAllPhase2CrudResources() {
        CrudResourceRegistry registry = new CrudResourceRegistry();

        assertThat(registry.get("cities").tableName()).isEqualTo("tm_city");
        assertThat(registry.get("attractions").tableName()).isEqualTo("tm_map_poi");
        assertThat(registry.get("hotels").tableName()).isEqualTo("tm_map_poi");
        assertThat(registry.get("restaurants").tableName()).isEqualTo("tm_map_poi");
        assertThat(registry.get("travel-tags").tableName()).isEqualTo("tm_travel_tag");
        assertThat(registry.get("favorites").tableName()).isEqualTo("tm_favorite");
        assertThat(registry.get("travel-notes").tableName()).isEqualTo("tm_travel_note");
        assertThat(registry.get("ai-records").tableName()).isEqualTo("tm_ai_analysis_record");
    }

    @Test
    void exposesAllowedColumnsForCityAndAttractionFilters() {
        CrudResourceRegistry registry = new CrudResourceRegistry();

        assertThat(registry.get("cities").searchColumns()).contains("name", "province", "description");
        assertThat(registry.get("attractions").filterColumns()).contains("city_id", "category", "tags", "rating");
        assertThat(registry.get("attractions").scopeValue()).isEqualTo("attraction");
    }
}
