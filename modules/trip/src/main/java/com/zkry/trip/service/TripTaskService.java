package com.zkry.trip.service;

import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import com.zkry.common.core.config.TravelMindSettingKeys;
import com.zkry.common.core.exception.BizException;
import com.zkry.content.dto.ContentPlanningContext;
import com.zkry.map.dto.MapPlanningContext;
import com.zkry.trip.constant.TripTaskMessages;
import com.zkry.trip.dto.SubmitTripPlanResponse;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.RouteIntent;
import com.zkry.trip.dto.RouteNode;
import com.zkry.trip.dto.TripTaskEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 旅行规划任务状态机。
 *
 * <p>Controller 只负责提交请求；真正的异步执行、阶段推进、WebSocket 事件推送都在这里。
 * 它不直接实现小红书/高德/LLM 细节，而是按阶段调用 {@link TripResearchService}
 * 和 {@link TripAiPlannerService}，让主流程保持可读。
 */
@Service
public class TripTaskService {

    private static final Logger log = LoggerFactory.getLogger(TripTaskService.class);

    private final Map<String, TripTaskState> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final TripAiPlannerService tripAiPlannerService;
    private final TripResearchService tripResearchService;
    private final TravelMindRuntimeSettingsService runtimeSettingsService;
    private final DemoTripPlannerService demoTripPlannerService;
    private final TripPlanPersistenceService tripPlanPersistenceService;
    private final TripPlanReviewer tripPlanReviewer;
    private final TravelAiApplicationService travelAiApplicationService;
    private final boolean xhsEnabled;

    public TripTaskService(
        TripAiPlannerService tripAiPlannerService,
        TripResearchService tripResearchService,
        TravelMindRuntimeSettingsService runtimeSettingsService,
        DemoTripPlannerService demoTripPlannerService,
        TripPlanPersistenceService tripPlanPersistenceService,
        TripPlanReviewer tripPlanReviewer,
        TravelAiApplicationService travelAiApplicationService,
        @Value("${travelmind.content.xhs.enabled:false}") boolean xhsEnabled
    ) {
        this.tripAiPlannerService = tripAiPlannerService;
        this.tripResearchService = tripResearchService;
        this.runtimeSettingsService = runtimeSettingsService;
        this.demoTripPlannerService = demoTripPlannerService;
        this.tripPlanPersistenceService = tripPlanPersistenceService;
        this.tripPlanReviewer = tripPlanReviewer;
        this.travelAiApplicationService = travelAiApplicationService;
        this.xhsEnabled = xhsEnabled;
    }

    /**
     * 提交旅行规划任务。
     *
     * <p>接口不会同步等 LLM 全部跑完，而是立刻返回 taskId 和 WebSocket 地址。
     * 真正耗时的资料研究、规划、图谱构建会在后台线程里执行。
     */
    public SubmitTripPlanResponse submit(TripRequest request, long userId) {
        validateTripRequest(request);
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        TripTaskState state = new TripTaskState(taskId, userId, request);
        tasks.put(taskId, state);
        log.info("[TripTask] 创建旅行规划任务 taskId={} cities={} totalDays={} date={}~{} preferences={} aiAvailable={}",
            taskId,
            request.normalizedCities().stream().map(city -> city.city() + ":" + city.safeDays() + "天").toList(),
            request.safeTravelDays(),
            safeLog(request.start_date()),
            safeLog(request.end_date()),
            request.safePreferences(),
            tripAiPlannerService.isAvailable());
        update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.SUBMITTED, 5, TripTaskMessages.SUBMITTED, null, null);
        CompletableFuture.runAsync(() -> runPlanning(taskId, userId, request), executorService);
        return new SubmitTripPlanResponse(
            taskId,
            taskId,
            TripTaskStatus.PROCESSING,
            "/api/user/trip/ws/" + taskId,
            TripTaskMessages.SUBMITTED
        );
    }

    public TripTaskEvent snapshot(String taskId, long userId) {
        TripTaskState state = task(taskId, userId);
        return state.toEvent(true);
    }

    public Map<String, Object> status(String taskId, long userId) {
        TripTaskState state = task(taskId, userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_id", state.taskId);
        payload.put("plan_id", state.taskId);
        payload.put("status", state.status);
        if (TripTaskStatus.COMPLETED.equals(state.status)) {
            payload.put("result", state.result);
            return payload;
        }
        if (TripTaskStatus.FAILED.equals(state.status)) {
            payload.put("error", state.error);
            payload.put("request_payload", state.requestPayload());
            return payload;
        }
        payload.put("stage", state.stage);
        payload.put("progress", state.progress);
        payload.put("progress_text", state.message);
        return payload;
    }

    public TripTaskSubscription subscribe(String taskId, long userId, TripTaskSubscriber subscriber) {
        TripTaskState state = task(taskId, userId);
        state.subscribers.add(subscriber);
        log.info("[TripTask] 新增任务订阅 taskId={} subscriberCount={}", taskId, state.subscribers.size());
        return new TripTaskSubscription(taskId, () -> {
            state.subscribers.remove(subscriber);
            log.info("[TripTask] 取消任务订阅 taskId={} subscriberCount={}", taskId, state.subscribers.size());
        });
    }

    private TripTaskState task(String taskId) {
        TripTaskState state = tasks.get(taskId);
        if (state == null) {
            throw new TripTaskNotFoundException(taskId);
        }
        return state;
    }

    private TripTaskState task(String taskId, long userId) {
        TripTaskState state = task(taskId);
        if (state.userId != userId) {
            throw new TripTaskNotFoundException(taskId);
        }
        return state;
    }

    /**
     * 后台任务主流程。
     *
     * <p>这是整套 Java 版 Travel Mind 的最重要阅读入口：先进入资料研究 Agent，
     * 再进入 Planner/Review Agent，最后把结构化结果推给前端。
     */
    private void runPlanning(String taskId, long userId, TripRequest request) {
        long startedAt = System.currentTimeMillis();
        try {
            log.info("[TripTask] 开始执行任务 taskId={} city={} language={} transportation={} accommodation={}",
                taskId, request.primaryCity(), request.safeLanguage(), request.safeTransportation(), request.safeAccommodation());
            pause();
            update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.INITIALIZING, 10, TripTaskMessages.INITIALIZING, null, null);
            pause();
            TripPlanResponse response;
            if (runtimeSettingsComplete()) {
                response = runRealPlanner(taskId, request);
            } else {
                log.info("[TripTask] 外部配置不完整，启用 MySQL Demo Planner taskId={} missing={}", taskId, missingRuntimeSettings());
                pause();
                update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.TRAVEL_RESEARCH, 24,
                    "正在读取本地行程资源和免费公开数据...", null, null);
                pause();
                update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.PLANNING, 85,
                    "正在整理公开天气、地点与演示参考预算...", null, null);
                response = demoTripPlannerService.plan(taskId, request);
            }
            TripPlanReviewer.ReviewOutcome review = tripPlanReviewer.review(response.data(), request);
            if (!review.passed()) {
                throw new BizException("行程结构校验未通过：" + String.join("；", review.issues()));
            }
            long savedPlanId = tripPlanPersistenceService.save(userId, response, request);
            evaluateComfort(userId, savedPlanId, response, request);
            TripPlanResponse savedResponse = new TripPlanResponse(
                response.success(),
                response.message(),
                String.valueOf(savedPlanId),
                response.data(),
                response.graph_data()
            );
            log.info("[TripTask] 规划结果生成 taskId={} days={} graphNodes={}",
                taskId,
                savedResponse.data() == null || savedResponse.data().days() == null ? 0 : savedResponse.data().days().size(),
                savedResponse.graph_data() == null || savedResponse.graph_data().nodes() == null ? 0 : savedResponse.graph_data().nodes().size());
            pause();
            update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.GRAPH_BUILDING, 95, TripTaskMessages.GRAPH_BUILDING, null, null);
            pause();
            update(taskId, TripTaskStatus.COMPLETED, TripTaskStage.COMPLETED, 100, TripTaskMessages.COMPLETED, savedResponse, null);
            log.info("[TripTask] 任务执行完成 taskId={} savedPlanId={} elapsedMs={}", taskId, savedPlanId,
                System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            log.error("[TripTask] 任务执行失败 taskId={} elapsedMs={} reason={}",
                taskId, System.currentTimeMillis() - startedAt, ex.getMessage(), ex);
            update(taskId, TripTaskStatus.FAILED, TripTaskStage.FAILED, 100, TripTaskMessages.FAILED, null, ex.getMessage());
        }
    }

    private void evaluateComfort(long userId, long savedPlanId, TripPlanResponse response, TripRequest request) {
        try {
            travelAiApplicationService.evaluateSavedTrip(userId, savedPlanId, response.data(), request);
        } catch (Exception ex) {
            log.warn("[TripTask] Python AI 舒适度评分失败但不阻断行程 taskPlanId={} reason={}", savedPlanId, ex.getMessage());
        }
    }

    private TripPlanResponse runRealPlanner(String taskId, TripRequest request) {
        update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.TRAVEL_RESEARCH, 24, TripTaskMessages.TRAVEL_RESEARCH, null, null);
        TripResearchService.ResearchContext researchContext = tripResearchService.research(taskId, request);
        ContentPlanningContext contentContext = researchContext.contentContext();
        MapPlanningContext mapContext = researchContext.mapContext();
        log.info("[TripTask] 资料研究阶段完成 taskId={} mapRealData={} mapCities={} contentRealData={} contentCities={} summary={}",
            taskId,
            mapContext.realData(),
            mapContext.safeCities().size(),
            contentContext.realData(),
            contentContext.safeCities().size(),
            researchContext.researchResult().safeSummary());
        if (!mapContext.realData()) {
            throw new BizException("高德地图上下文采集失败：" + mapContext.message());
        }
        if (xhsEnabled && !contentContext.realData()) {
            throw new BizException("小红书内容采集失败：" + contentContext.message());
        }
        pause();
        update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.WEATHER_SEARCH, 46, mapStageMessage(mapContext, "天气"), null, null);
        pause();
        update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.HOTEL_SEARCH, 64, mapStageMessage(mapContext, "酒店和餐饮"), null, null);
        pause();
        update(taskId, TripTaskStatus.PROCESSING, TripTaskStage.PLANNING, 85, TripTaskMessages.PLANNING, null, null);
        return tripAiPlannerService.plan(taskId, request, mapContext, contentContext)
            .orElseGet(() -> {
                log.warn("[TripTask] 真实 Planner 未生成可解析结果，降级到 MySQL Demo Planner taskId={}", taskId);
                return demoTripPlannerService.plan(taskId, request);
            });
    }

    void validateTripRequest(TripRequest request) {
        if (request == null) {
            throw new BizException("行程请求不能为空。");
        }
        if (request.normalizedCities().isEmpty()) {
            throw new BizException("请至少填写一个目的地城市。");
        }
        if (request.safeTravelDays() <= 0 || request.safeTravelDays() > 30) {
            throw new BizException("旅行天数必须在 1 到 30 天之间。");
        }
        try {
            LocalDate startDate = LocalDate.parse(request.start_date());
            LocalDate endDate = LocalDate.parse(request.end_date());
            if (startDate.isBefore(LocalDate.now())) {
                throw new BizException("出发日期不能早于今天。");
            }
            if (endDate.isBefore(startDate)) {
                throw new BizException("返程日期不能早于出发日期。");
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) + 1 != request.safeTravelDays()) {
                throw new BizException("返程日期与旅行天数不一致。");
            }
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new BizException("请填写有效的出发和返程日期。");
        }
        validateRouteIntent(request);
    }

    private void validateRouteIntent(TripRequest request) {
        RouteIntent intent = request.route_intent();
        if (intent == null) return;
        if (!List.of("soft_order", "strict_order").contains(intent.mode())) {
            throw new BizException("路线规划方式无效。");
        }
        if (intent.city() == null || !intent.city().trim().equals(request.primaryCity().trim())) {
            throw new BizException("路线草稿与目的地城市不一致。");
        }
        List<RouteNode> nodes = intent.safeNodes();
        if (nodes.size() < 2 || nodes.size() > 30) {
            throw new BizException("路线节点必须在 2 到 30 个之间。");
        }
        for (int index = 0; index < nodes.size(); index++) {
            RouteNode node = nodes.get(index);
            if (node == null || node.order() == null || node.order() != index + 1) {
                throw new BizException("路线节点顺序必须从 1 连续编号。");
            }
            if (!List.of("poi", "free_point").contains(node.type())) {
                throw new BizException("路线节点类型无效。");
            }
            if (node.name() == null || node.name().isBlank() || node.name().length() > 120
                || node.poi_id() != null && node.poi_id().length() > 120
                || node.kind() != null && node.kind().length() > 40
                || node.note() != null && node.note().length() > 240
                || node.safePreferences().size() > 6
                || node.safePreferences().stream().anyMatch(item -> item == null || item.isBlank() || item.length() > 20)) {
                throw new BizException("路线节点名称或标识无效。");
            }
            if (node.longitude() == null || !Double.isFinite(node.longitude()) || node.longitude() < -180 || node.longitude() > 180
                || node.latitude() == null || !Double.isFinite(node.latitude()) || node.latitude() < -90 || node.latitude() > 90) {
                throw new BizException("路线节点经纬度无效。");
            }
        }
    }

    private boolean runtimeSettingsComplete() {
        return missingRuntimeSettings().isEmpty();
    }

    private List<String> missingRuntimeSettings() {
        List<String> missing = new ArrayList<>();
        if (xhsEnabled && !runtimeSettingsService.hasText(TravelMindSettingKeys.XHS_COOKIE)) {
            missing.add("小红书 Cookie");
        }
        if (!runtimeSettingsService.hasText(TravelMindSettingKeys.AMAP_WEB_KEY)) {
            missing.add("高德地图 Web Service Key");
        }
        if (!runtimeSettingsService.hasText(TravelMindSettingKeys.OPENAI_API_KEY)) {
            missing.add("AI API Key");
        }
        if (!runtimeSettingsService.hasText(TravelMindSettingKeys.OPENAI_MODEL)) {
            missing.add("AI 模型名称");
        }
        return missing;
    }

    private String mapStageMessage(MapPlanningContext mapContext, String subject) {
        if (mapContext.realData()) {
            return "已获取地图" + subject + "上下文，正在整理给规划智能体...";
        }
        return mapContext.message() + " 正在继续准备" + subject + "候选信息。";
    }

    /**
     * 更新内存任务状态，并推送给所有 WebSocket 订阅者。
     *
     * <p>前端进度条、轮询接口和最终结果都来自这里维护的 {@link TripTaskState}。
     */
    private void update(
        String taskId,
        String status,
        String stage,
        int progress,
        String message,
        TripPlanResponse result,
        String error
    ) {
        TripTaskState state = tasks.get(taskId);
        if (state == null) {
            return;
        }
        log.info("[TripTask] 进度更新 taskId={} status={} stage={} progress={} message={}",
            taskId, status, stage, progress, message);
        state.status = status;
        state.stage = stage;
        state.progress = progress;
        state.message = message;
        if (result != null) {
            state.result = result;
        }
        if (error != null) {
            state.error = error;
        }
        TripTaskEvent event = state.toEvent(true);
        for (TripTaskSubscriber subscriber : state.subscribers) {
            subscriber.onEvent(event);
        }
    }

    private void pause() {
        try {
            Thread.sleep(450L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("任务被中断", ex);
        }
    }

    private String safeLog(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static final class TripTaskState {

        private final String taskId;
        private final long userId;
        private final TripRequest request;
        private final CopyOnWriteArrayList<TripTaskSubscriber> subscribers = new CopyOnWriteArrayList<>();

        private volatile String status = TripTaskStatus.PROCESSING;
        private volatile String stage = TripTaskStage.SUBMITTED;
        private volatile int progress = 0;
        private volatile String message = "";
        private volatile String error = "";
        private volatile TripPlanResponse result;

        private TripTaskState(String taskId, long userId, TripRequest request) {
            this.taskId = taskId;
            this.userId = userId;
            this.request = request;
        }

        private TripTaskEvent toEvent(boolean includeResult) {
            return new TripTaskEvent(
                taskId,
                taskId,
                status,
                stage,
                progress,
                message,
                error == null || error.isBlank() ? null : error,
                includeResult ? result : null,
                TripTaskStatus.FAILED.equals(status) ? requestPayload() : null
            );
        }

        private Map<String, Object> requestPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("city", request.city());
            payload.put("cities", request.cities());
            payload.put("start_date", request.start_date());
            payload.put("end_date", request.end_date());
            payload.put("travel_days", request.travel_days());
            payload.put("transportation", request.transportation());
            payload.put("accommodation", request.accommodation());
            payload.put("budget", request.budget());
            payload.put("preferences", request.preferences());
            payload.put("free_text_input", request.free_text_input());
            payload.put("language", request.language());
            payload.put("route_intent", request.route_intent());
            return payload;
        }
    }
}
