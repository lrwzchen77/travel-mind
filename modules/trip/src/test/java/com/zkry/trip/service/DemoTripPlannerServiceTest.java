package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class DemoTripPlannerServiceTest {

    @Test
    void createsStructuredPlanFromSeedResources() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Map.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("tm_city")) {
                return List.of(Map.of("id", 2001L, "name", "Hangzhou", "province", "Zhejiang"));
            }
            if (sql.contains("tm_attraction")) {
                return List.of(
                    Map.of("name", "West Lake", "address", "Xihu", "category", "nature", "rating", new BigDecimal("4.9"),
                        "price", new BigDecimal("0.00"), "description", "Classic lake walk"),
                    Map.of("name", "Lingyin Temple", "address", "Fayun", "category", "culture", "rating", new BigDecimal("4.7"),
                        "price", new BigDecimal("45.00"), "description", "Temple visit")
                );
            }
            if (sql.contains("tm_hotel")) {
                return List.of(Map.of("name", "West Lake Hotel", "address", "Xihu", "rating", new BigDecimal("4.6"),
                    "price_range", "500-700", "category", "hotel"));
            }
            if (sql.contains("tm_restaurant")) {
                return List.of(Map.of("name", "Lakeside Restaurant", "address", "Hubin", "average_cost", new BigDecimal("90.00"),
                    "cuisine", "Hangzhou cuisine", "description", "Local dishes"));
            }
            if (sql.contains("tm_travel_note")) {
                return List.of(Map.of("title", "Morning route", "content", "Start early around West Lake."));
            }
            return List.of();
        });
        DemoTripPlannerService planner = new DemoTripPlannerService(jdbcTemplate, new TripPlanReviewer());

        TripPlanResponse response = planner.plan("demo-1", request());

        assertThat(response.success()).isTrue();
        assertThat(response.plan_id()).isEqualTo("demo-1");
        assertThat(response.data().city()).isEqualTo("Hangzhou");
        assertThat(response.data().days()).hasSize(2);
        assertThat(response.data().days().get(0).attractions()).isNotEmpty();
        assertThat(response.data().days().get(0).meals()).isNotEmpty();
        assertThat(response.data().budget().total()).isGreaterThan(0);
    }

    private TripRequest request() {
        return new TripRequest("Hangzhou", null, "2026-08-01", "2026-08-02", 2, "公共交通", "舒适型酒店", "3000",
            List.of("湖景", "美食"), "节奏轻松", "zh");
    }
}
