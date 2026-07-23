package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

import com.zkry.common.json.utils.JsonUtils;
import com.zkry.map.dto.PublicDataItem;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.InspirationSource;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.RouteIntent;
import com.zkry.trip.dto.RouteNode;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.mockito.ArgumentCaptor;

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

    @Test
    void scopesTripReadsAndDeletesToCurrentUser() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        TripPlanResponse response = response("plan-1");
        when(jdbcTemplate.queryForList(contains("user_id = :userId"), eq(Map.of("id", 9001L, "userId", 1001L))))
            .thenReturn(List.of(Map.of("raw_plan_json", JsonUtils.toJsonString(response))));
        when(jdbcTemplate.update(contains("user_id = :userId"), any(MapSqlParameterSource.class))).thenReturn(0);
        TripPlanPersistenceService service = new TripPlanPersistenceService(jdbcTemplate);

        assertThat(service.detail(9001L, 1001L).data().city()).isEqualTo("Hangzhou");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.delete(9001L, 2002L))
            .isInstanceOf(com.zkry.common.core.exception.BizException.class)
            .hasMessage("行程不存在或无权操作。");
    }

    @Test
    void persistsVerifiedInspirationSourcesWithPlanSnapshot() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        TripPlanPersistenceService service = new TripPlanPersistenceService(jdbcTemplate);
        TripRequest request = new TripRequest("Hangzhou", null, "2026-08-01", "2026-08-01", 1, "公共交通", "舒适型酒店", "2000",
            List.of("湖景"), "参考旅行灵感", "zh", List.of(7001L),
            List.of(new InspirationSource(7001L, "西湖慢游", "Hangzhou", "route", "must", "上午走断桥和苏堤")),
            new RouteIntent("Hangzhou", "soft_order", List.of(
                new RouteNode(1, "poi", "west-lake", "West Lake", 120.1485, 30.242, "attraction", "Sunset stop", List.of("must", "photo")),
                new RouteNode(2, "free_point", null, "Custom point 2", 120.1152, 30.2288, null)
            )));

        service.save(1001L, response("plan-1"), request);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(contains("INSERT INTO tm_trip_plan"), params.capture());
        TripPlanResponse stored = JsonUtils.parseObject(String.valueOf(params.getValue().getValue("rawPlanJson")), TripPlanResponse.class);
        assertThat(stored.data().inspiration_sources()).singleElement().extracting(InspirationSource::intent).isEqualTo("must");
        assertThat(stored.data().public_data()).singleElement().extracting(PublicDataItem::data_kind).isEqualTo("live");
        assertThat(stored.data().route_intent().nodes()).hasSize(2);
        assertThat(stored.data().route_intent().nodes().get(0).poi_id()).isEqualTo("west-lake");
        assertThat(stored.data().route_intent().nodes().get(0).note()).isEqualTo("Sunset stop");
        assertThat(stored.data().route_intent().nodes().get(0).preferences()).containsExactly("must", "photo");
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
            "注意防晒。", new Budget(0, 600, 80, 40, 0, 720), List.of(), List.of(
                new PublicDataItem("当前天气", "杭州 31°C", "Open-Meteo", "2026-08-01T10:00:00+08:00", "live", false, "")
            ));
        return TripPlanResponseFactory.fromPlan(planId, plan);
    }
}
