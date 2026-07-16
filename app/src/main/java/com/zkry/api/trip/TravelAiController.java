package com.zkry.api.trip;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.trip.dto.ai.ContentAnalyzeRequest;
import com.zkry.trip.dto.ai.ContentAnalyzeResult;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.trip.service.TravelAiApplicationService;
import com.zkry.trip.service.TripPlanPersistenceService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/user/ai", "/api/admin/ai"})
public class TravelAiController {

    private final TravelAiApplicationService travelAiApplicationService;
    private final TripPlanPersistenceService tripPlanPersistenceService;

    public TravelAiController(
        TravelAiApplicationService travelAiApplicationService,
        TripPlanPersistenceService tripPlanPersistenceService
    ) {
        this.travelAiApplicationService = travelAiApplicationService;
        this.tripPlanPersistenceService = tripPlanPersistenceService;
    }

    @PostMapping("/vision/detect")
    public R<PythonAiCallResult<VisionDetectResult>> detectVision(
        @RequestBody VisionDetectRequest request
    ) {
        return R.ok(travelAiApplicationService.detectVision(LoginHelper.getUserId(), request));
    }

    @PostMapping("/trip/evaluate")
    public R<PythonAiCallResult<TripEvaluateResult>> evaluateTrip(
        @RequestBody TripEvaluateRequest request,
        @RequestParam(defaultValue = "trip_plan") String targetType,
        @RequestParam(required = false) Long targetId
    ) {
        return R.ok(travelAiApplicationService.evaluateTrip(LoginHelper.getUserId(), targetType, targetId, request));
    }

    @PostMapping("/content/analyze")
    public R<PythonAiCallResult<ContentAnalyzeResult>> analyzeContent(
        @RequestBody ContentAnalyzeRequest request,
        @RequestParam(defaultValue = "travel_note") String targetType,
        @RequestParam(required = false) Long targetId
    ) {
        return R.ok(travelAiApplicationService.analyzeContent(LoginHelper.getUserId(), targetType, targetId, request));
    }

    @GetMapping("/trip/{id}/comfort")
    public R<Map<String, Object>> tripComfort(@PathVariable long id) {
        tripPlanPersistenceService.detail(id, LoginHelper.getUserId());
        return R.ok(travelAiApplicationService.latestTripComfort(id));
    }
}
