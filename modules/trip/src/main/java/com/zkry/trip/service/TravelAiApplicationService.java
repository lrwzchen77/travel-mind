package com.zkry.trip.service;

import com.zkry.trip.dto.ai.ContentAnalyzeRequest;
import com.zkry.trip.dto.ai.ContentAnalyzeResult;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripDayEvaluationInput;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TravelAiApplicationService {

    private final PythonAiClient pythonAiClient;
    private final AiAnalysisRecordService recordService;

    public TravelAiApplicationService(PythonAiClient pythonAiClient, AiAnalysisRecordService recordService) {
        this.pythonAiClient = pythonAiClient;
        this.recordService = recordService;
    }

    public PythonAiCallResult<VisionDetectResult> detectVision(Long userId, VisionDetectRequest request) {
        PythonAiCallResult<VisionDetectResult> result = pythonAiClient.detectVision(request);
        recordService.save(
            userId,
            "vision_detect",
            request == null ? null : request.resource_type(),
            null,
            "image=" + safe(request == null ? null : request.image_url()) + ", city=" + safe(request == null ? null : request.city()),
            result
        );
        return result;
    }

    public PythonAiCallResult<TripEvaluateResult> evaluateTrip(
        Long userId,
        String targetType,
        Long targetId,
        TripEvaluateRequest request
    ) {
        PythonAiCallResult<TripEvaluateResult> result = pythonAiClient.evaluateTrip(request);
        recordService.save(
            userId,
            "trip_evaluate",
            targetType == null || targetType.isBlank() ? "trip_plan" : targetType,
            targetId,
            "days=" + (request == null || request.days() == null ? 0 : request.days().size())
                + ", transportation=" + safe(request == null ? null : request.transportation()),
            result
        );
        return result;
    }

    public PythonAiCallResult<ContentAnalyzeResult> analyzeContent(
        Long userId,
        String targetType,
        Long targetId,
        ContentAnalyzeRequest request
    ) {
        PythonAiCallResult<ContentAnalyzeResult> result = pythonAiClient.analyzeContent(request);
        recordService.save(
            userId,
            "content_analyze",
            targetType == null || targetType.isBlank() ? "travel_note" : targetType,
            targetId,
            "city=" + safe(request == null ? null : request.city()) + ", attraction=" + safe(request == null ? null : request.attraction_name()),
            result
        );
        return result;
    }

    public PythonAiCallResult<TripEvaluateResult> evaluateSavedTrip(
        Long userId,
        long tripPlanId,
        TripPlan plan,
        TripRequest request
    ) {
        return evaluateTrip(userId, "trip_plan", tripPlanId, toEvaluateRequest(plan, request));
    }

    public Map<String, Object> latestTripComfort(long tripPlanId, long userId) {
        return recordService.latest("trip_evaluate", "trip_plan", tripPlanId, userId);
    }

    private TripEvaluateRequest toEvaluateRequest(TripPlan plan, TripRequest request) {
        List<DayPlan> days = plan == null || plan.days() == null ? List.of() : plan.days();
        List<WeatherInfo> weather = plan == null || plan.weather_info() == null ? List.of() : plan.weather_info();
        List<TripDayEvaluationInput> dayInputs = days.stream()
            .map(day -> new TripDayEvaluationInput(
                day.date(),
                day.city(),
                day.attractions() == null ? List.of() : day.attractions().stream().map(Attraction::name).toList(),
                weatherForDate(weather, day.date()),
                Boolean.TRUE.equals(day.is_transfer_day())
            ))
            .toList();
        int transfers = (int) days.stream().filter(day -> Boolean.TRUE.equals(day.is_transfer_day())).count();
        return new TripEvaluateRequest(
            dayInputs,
            request == null ? "" : request.safeTransportation(),
            transfers,
            request == null ? List.of() : request.safePreferences(),
            plan == null || plan.budget() == null || plan.budget().total() == null ? null : plan.budget().total().doubleValue()
        );
    }

    private String weatherForDate(List<WeatherInfo> weather, String date) {
        return weather.stream()
            .filter(item -> date != null && date.equals(item.date()))
            .findFirst()
            .map(item -> safe(item.day_weather()) + " " + safe(item.night_weather()))
            .orElse("");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
