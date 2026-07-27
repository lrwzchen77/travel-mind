package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TravelAiApplicationServiceTest {

    @Test
    void detectVisionPersistsSuccessfulAnalysisRecord() {
        PythonAiClient client = org.mockito.Mockito.mock(PythonAiClient.class);
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        AiAnalysisRecordService recordService = new AiAnalysisRecordService(jdbcTemplate);
        TravelAiApplicationService service = new TravelAiApplicationService(client, recordService);
        VisionDetectResult result = new VisionDetectResult("rule", List.of(), List.of("food"), "Hangzhou food image", List.of(), "image_url");
        when(client.detectVision(any())).thenReturn(PythonAiCallResult.ok("success", result, "{\"data\":{}}"));

        PythonAiCallResult<VisionDetectResult> response = service.detectVision(
            1001L,
            new VisionDetectRequest("https://example.com/food.jpg", "Hangzhou", "restaurant")
        );

        assertThat(response.success()).isTrue();
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("tm_ai_analysis_record"), captor.capture());
        assertThat(captor.getValue().getValue("analysisType")).isEqualTo("vision_detect");
        assertThat(captor.getValue().getValue("targetType")).isEqualTo("restaurant");
        assertThat(captor.getValue().getValue("status")).isEqualTo("success");
    }

    @Test
    void evaluateTripPersistsFailedAnalysisRecordWhenPythonFails() {
        PythonAiClient client = org.mockito.Mockito.mock(PythonAiClient.class);
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        AiAnalysisRecordService recordService = new AiAnalysisRecordService(jdbcTemplate);
        TravelAiApplicationService service = new TravelAiApplicationService(client, recordService);
        when(client.evaluateTrip(any())).thenReturn(PythonAiCallResult.failure("Python AI service unavailable"));

        PythonAiCallResult<TripEvaluateResult> response = service.evaluateTrip(1001L, null, null, new TripEvaluateRequest(
            List.of(), "公共交通", 0, List.of("轻松"), 2000D
        ));

        assertThat(response.success()).isFalse();
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("tm_ai_analysis_record"), captor.capture());
        assertThat(captor.getValue().getValue("analysisType")).isEqualTo("trip_evaluate");
        assertThat(captor.getValue().getValue("status")).isEqualTo("failed");
    }

    @Test
    void latestComfortScoreReturnsMostRecentRecord() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), any(Map.class))).thenReturn(List.of(Map.of(
            "id", 9001L,
            "result_json", "{\"comfort_score\":88,\"risk_level\":\"low\"}",
            "status", "success"
        )));
        AiAnalysisRecordService recordService = new AiAnalysisRecordService(jdbcTemplate);

        Map<String, Object> latest = recordService.latest("trip_evaluate", "trip_plan", 178L, 1001L);

        assertThat(latest.get("status")).isEqualTo("success");
        assertThat(String.valueOf(latest.get("result_json"))).contains("comfort_score");
    }

    @Test
    void evaluateSavedTripPersistsFailureWithoutThrowing() {
        PythonAiClient client = org.mockito.Mockito.mock(PythonAiClient.class);
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        AiAnalysisRecordService recordService = new AiAnalysisRecordService(jdbcTemplate);
        TravelAiApplicationService service = new TravelAiApplicationService(client, recordService);
        when(client.evaluateTrip(any())).thenReturn(PythonAiCallResult.failure("Python AI service unavailable"));

        PythonAiCallResult<TripEvaluateResult> result = service.evaluateSavedTrip(1001L, 9001L, completePlan(), request());

        assertThat(result.success()).isFalse();
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("tm_ai_analysis_record"), captor.capture());
        assertThat(captor.getValue().getValue("targetType")).isEqualTo("trip_plan");
        assertThat(captor.getValue().getValue("targetId")).isEqualTo(9001L);
        assertThat(captor.getValue().getValue("status")).isEqualTo("failed");
    }

    private TripRequest request() {
        return new TripRequest("Hangzhou", null, "2026-08-01", "2026-08-01", 1, "公共交通", "舒适型酒店", "2000",
            List.of("轻松"), "", "zh");
    }

    private TripPlan completePlan() {
        Hotel hotel = new Hotel("West Lake Hotel", "Hangzhou", null, "500-700", "4.7", "near lake", "舒适型", 600);
        return new TripPlan("Hangzhou", List.of("Hangzhou"), "2026-08-01", "2026-08-01", List.of(
            new DayPlan("2026-08-01", 0, "Hangzhou", false, "", "西湖慢行", "公共交通", "舒适型酒店", hotel,
                List.of(new Attraction("West Lake", "Hangzhou", null, 180, "湖景步行", "nature", 4.9, "", 0)),
                List.of(new Meal("午餐", "湖滨餐厅", "Hangzhou", null, "本地菜", 80)))
        ), List.of(new WeatherInfo("2026-08-01", "Hangzhou", "雨", "多云", 31, 24, "东风", "3级")),
            "注意防晒。", new Budget(0, 600, 80, 40, 0, 720));
    }
}
