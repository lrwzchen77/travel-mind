package com.zkry.api.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zkry.common.core.domain.R;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.trip.service.TravelAiApplicationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TravelAiControllerTest {

    @Test
    void detectVisionDelegatesToApplicationService() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TravelAiController controller = new TravelAiController(service);
        VisionDetectRequest request = new VisionDetectRequest("https://example.com/a.jpg", "Hangzhou", "attraction");
        when(service.detectVision(1001L, request)).thenReturn(PythonAiCallResult.ok("success",
            new VisionDetectResult("rule", List.of(), List.of("travel_scene"), "summary", List.of(), "image_url"),
            "{}"));

        R<PythonAiCallResult<VisionDetectResult>> response = controller.detectVision(request, 1001L);

        assertThat(response.getData().success()).isTrue();
        assertThat(response.getData().data().summary()).isEqualTo("summary");
    }

    @Test
    void latestTripComfortReturnsRecordMap() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TravelAiController controller = new TravelAiController(service);
        when(service.latestTripComfort(9001L)).thenReturn(Map.of("status", "success"));

        R<Map<String, Object>> response = controller.tripComfort(9001L);

        assertThat(response.getData()).containsEntry("status", "success");
    }

    @Test
    void evaluateTripDelegatesWithTripTarget() {
        TravelAiApplicationService service = org.mockito.Mockito.mock(TravelAiApplicationService.class);
        TravelAiController controller = new TravelAiController(service);
        TripEvaluateRequest request = new TripEvaluateRequest(List.of(), "公共交通", 0, List.of("轻松"), 2000D);
        when(service.evaluateTrip(1001L, "trip_plan", 99L, request)).thenReturn(PythonAiCallResult.ok("success",
            new TripEvaluateResult(88, "low", List.of(), List.of("ok")),
            "{}"));

        R<PythonAiCallResult<TripEvaluateResult>> response = controller.evaluateTrip(request, 1001L, "trip_plan", 99L);

        assertThat(response.getData().data().comfort_score()).isEqualTo(88);
    }
}
