package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.json.utils.JsonUtils;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TripPlanPersistenceServiceTest {

    @Test
    void savesLoadsCopiesAndDeletesPlan() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        TripPlanResponse response = response("plan-1");
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), any(Map.class))).thenReturn(List.of(Map.of(
            "id", 9001L,
            "raw_plan_json", JsonUtils.toJsonString(response)
        )));
        TripPlanPersistenceService service = new TripPlanPersistenceService(jdbcTemplate);

        long savedId = service.save(1001L, response, request());
        TripPlanResponse detail = service.detail(9001L);
        long copiedId = service.copy(9001L, 1001L);
        service.delete(copiedId);

        assertThat(savedId).isPositive();
        assertThat(detail.data().city()).isEqualTo("Hangzhou");
        assertThat(copiedId).isPositive();
        assertThat(copiedId).isNotEqualTo(9001L);
        verify(jdbcTemplate, atLeast(4)).update(anyString(), any(MapSqlParameterSource.class));
    }

    private TripRequest request() {
        return new TripRequest("Hangzhou", null, "2026-08-01", "2026-08-01", 1, "公共交通", "舒适型酒店", "2000",
            List.of("湖景"), "节奏轻松", "zh");
    }

    private TripPlanResponse response(String planId) {
        Hotel hotel = new Hotel("West Lake Hotel", "Hangzhou", null, "500-700", "4.7", "near lake", "舒适型", 600);
        TripPlan plan = new TripPlan("Hangzhou", List.of("Hangzhou"), "2026-08-01", "2026-08-01", List.of(
            new DayPlan("2026-08-01", 0, "Hangzhou", false, "", "西湖慢行", "公共交通", "舒适型酒店", hotel,
                List.of(new Attraction("West Lake", "Hangzhou", null, 180, "湖景步行", "nature", 4.9, "", 0)),
                List.of(new Meal("午餐", "湖滨餐厅", "Hangzhou", null, "本地菜", 80)))
        ), List.of(new WeatherInfo("2026-08-01", "Hangzhou", "晴", "多云", 31, 24, "东风", "3级")),
            "注意防晒。", new Budget(0, 600, 80, 40, 0, 720));
        return TripPlanResponseFactory.fromPlan(planId, plan);
    }
}
