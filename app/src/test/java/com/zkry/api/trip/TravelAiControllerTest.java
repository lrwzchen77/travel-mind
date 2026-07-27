package com.zkry.api.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.trip.service.TravelAiApplicationService;
import com.zkry.trip.service.TripPlanPersistenceService;
import com.zkry.resources.service.CrudResourceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TravelAiControllerTest {

    @Test
    void detectVisionDelegatesToApplicationService() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TripPlanPersistenceService persistence = org.mockito.Mockito.mock(TripPlanPersistenceService.class);
        TravelAiController controller = new TravelAiController(service, persistence, org.mockito.Mockito.mock(CrudResourceService.class));
        VisionDetectRequest request = new VisionDetectRequest("https://example.com/a.jpg", "Hangzhou", "attraction");
        when(service.detectVision(1001L, request)).thenReturn(PythonAiCallResult.ok("success",
            new VisionDetectResult("rule", List.of(), List.of("travel_scene"), "summary", List.of(), "image_url"),
            "{}"));

        R<PythonAiCallResult<VisionDetectResult>> response;
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            response = controller.detectVision(request);
        }

        assertThat(response.getData().success()).isTrue();
        assertThat(response.getData().data().summary()).isEqualTo("summary");
    }

    @Test
    void latestTripComfortReturnsRecordMap() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TripPlanPersistenceService persistence = org.mockito.Mockito.mock(TripPlanPersistenceService.class);
        TravelAiController controller = new TravelAiController(service, persistence, org.mockito.Mockito.mock(CrudResourceService.class));
        when(service.latestTripComfort(9001L, 1001L)).thenReturn(Map.of("status", "success"));

        R<Map<String, Object>> response;
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            response = controller.tripComfort(9001L);
        }

        assertThat(response.getData()).containsEntry("status", "success");
    }

    @Test
    void evaluateTripDelegatesWithTripTarget() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TripPlanPersistenceService persistence = org.mockito.Mockito.mock(TripPlanPersistenceService.class);
        TravelAiController controller = new TravelAiController(service, persistence, org.mockito.Mockito.mock(CrudResourceService.class));
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getRequestURI()).thenReturn("/api/user/ai/trip/evaluate");
        TripEvaluateRequest request = new TripEvaluateRequest(List.of(), "公共交通", 0, List.of("轻松"), 2000D);
        when(service.evaluateTrip(1001L, "trip_plan", 99L, request)).thenReturn(PythonAiCallResult.ok("success",
            new TripEvaluateResult("trained_travel_comfort", "travel-comfort-v1", "relaxed", 0.9,
                Map.of("relaxed", 0.9), Map.of(), "bootstrap_scenarios_v1", 88, "low", List.of(), List.of("ok")),
            "{}"));

        R<PythonAiCallResult<TripEvaluateResult>> response;
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            response = controller.evaluateTrip(request, "trip_plan", 99L, httpRequest);
        }

        assertThat(response.getData().data().comfort_score()).isEqualTo(88);
    }
}
