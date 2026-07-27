package com.zkry.api.trip;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.common.core.exception.BizException;
import com.zkry.resources.service.CrudResourceService;
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
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class TravelAiController {

    private final TravelAiApplicationService travelAiApplicationService;
    private final TripPlanPersistenceService tripPlanPersistenceService;
    private final CrudResourceService resources;

    public TravelAiController(
        TravelAiApplicationService travelAiApplicationService,
        TripPlanPersistenceService tripPlanPersistenceService,
        CrudResourceService resources
    ) {
        this.travelAiApplicationService = travelAiApplicationService;
        this.tripPlanPersistenceService = tripPlanPersistenceService;
        this.resources = resources;
    }

    @PostMapping({"/user/ai/vision/detect", "/admin/ai/vision/detect"})
    public R<PythonAiCallResult<VisionDetectResult>> detectVision(
        @RequestBody VisionDetectRequest request
    ) {
        return R.ok(travelAiApplicationService.detectVision(LoginHelper.getUserId(), request));
    }

    @PostMapping("/admin/ai/trip/evaluate")
    public R<PythonAiCallResult<TripEvaluateResult>> evaluateTrip(
        @RequestBody TripEvaluateRequest request,
        @RequestParam(defaultValue = "trip_plan") String targetType,
        @RequestParam(required = false) Long targetId
    ) {
        return R.ok(travelAiApplicationService.evaluateTrip(LoginHelper.getUserId(), targetType, targetId, request));
    }

    @PostMapping({"/user/ai/content/analyze", "/admin/ai/content/analyze"})
    public R<PythonAiCallResult<ContentAnalyzeResult>> analyzeContent(
        @RequestBody ContentAnalyzeRequest request,
        @RequestParam(defaultValue = "travel_note") String targetType,
        @RequestParam(required = false) Long targetId,
        HttpServletRequest httpRequest
    ) {
        requireOwnedTarget(httpRequest, targetType, targetId);
        return R.ok(travelAiApplicationService.analyzeContent(LoginHelper.getUserId(), targetType, targetId, request));
    }

    @GetMapping("/user/ai/trip/{id}/comfort")
    public R<Map<String, Object>> tripComfort(@PathVariable long id) {
        tripPlanPersistenceService.detail(id, LoginHelper.getUserId());
        return R.ok(travelAiApplicationService.latestTripComfort(id, LoginHelper.getUserId()));
    }

    private void requireOwnedTarget(HttpServletRequest request, String targetType, Long targetId) {
        if (!request.getRequestURI().startsWith("/api/user/") || targetId == null) return;
        long userId = LoginHelper.getUserId();
        if ("trip_plan".equals(targetType)) {
            tripPlanPersistenceService.detail(targetId, userId);
            return;
        }
        if ("travel_note".equals(targetType)) {
            Object owner = resources.detail("travel-notes", targetId).get("user_id");
            if (owner instanceof Number number && number.longValue() == userId) return;
        }
        throw new BizException("AI 分析目标不存在或无权操作。");
    }
}
