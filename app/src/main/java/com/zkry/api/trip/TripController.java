package com.zkry.api.trip;

import com.zkry.trip.dto.SubmitTripPlanResponse;
import com.zkry.trip.dto.TripChatRequest;
import com.zkry.trip.dto.TripChatResponse;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.service.TripChatService;
import com.zkry.trip.service.TripPlanPersistenceService;
import com.zkry.trip.service.TripTaskService;
import com.zkry.common.core.domain.PageResult;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.TripHistoryPersistenceService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/trip")
public class TripController {

    private static final Logger log = LoggerFactory.getLogger(TripController.class);

    private final TripTaskService tripTaskService;

    private final TripHistoryPersistenceService tripHistoryPersistenceService;

    private final TripPlanPersistenceService tripPlanPersistenceService;

    private final TripChatService tripChatService;

    public TripController(
        TripTaskService tripTaskService,
        TripHistoryPersistenceService tripHistoryPersistenceService,
        TripPlanPersistenceService tripPlanPersistenceService,
        TripChatService tripChatService
    ) {
        this.tripTaskService = tripTaskService;
        this.tripHistoryPersistenceService = tripHistoryPersistenceService;
        this.tripPlanPersistenceService = tripPlanPersistenceService;
        this.tripChatService = tripChatService;
    }

    @PostMapping("/plan")
    public SubmitTripPlanResponse plan(@RequestBody TripRequest request) {
        log.info("[TripAPI] 收到行程规划请求 city={} cities={} days={} date={}~{} preferences={}",
            request.primaryCity(),
            request.normalizedCities().stream().map(city -> city.city() + ":" + city.safeDays() + "天").toList(),
            request.safeTravelDays(),
            safe(request.start_date()),
            safe(request.end_date()),
            request.safePreferences());
        SubmitTripPlanResponse response = tripTaskService.submit(request, LoginHelper.getUserId());
        log.info("[TripAPI] 行程规划任务已提交 taskId={} wsUrl={}", response.task_id(), response.ws_url());
        return response;
    }

    @GetMapping("/status/{taskId}")
    public Map<String, Object> status(@PathVariable String taskId) {
        Map<String, Object> payload = tripTaskService.status(taskId, LoginHelper.getUserId());
        log.info("[TripAPI] 查询任务状态 taskId={} status={} stage={} progress={}",
            taskId, payload.get("status"), payload.getOrDefault("stage", "-"), payload.getOrDefault("progress", "-"));
        return payload;
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestParam(defaultValue = "8") int limit) {
        PageResult<Map<String, Object>> history = tripHistoryPersistenceService.history(LoginHelper.getUserId(), limit);
        log.info("[TripAPI] 查询历史行程 limit={} total={}", limit, history.getTotal());
        return Map.of(
            "items", history.getRecords(),
            "total", history.getTotal(),
            "pageNum", history.getPageNum(),
            "pageSize", history.getPageSize()
        );
    }

    @GetMapping("/{id}")
    public TripPlanResponse detail(@PathVariable long id) {
        log.info("[TripAPI] 查询行程详情 id={}", id);
        return tripPlanPersistenceService.detail(id, LoginHelper.getUserId());
    }

    @PostMapping("/{id}/copy")
    public Map<String, Object> copy(@PathVariable long id) {
        long userId = LoginHelper.getUserId();
        long copiedId = tripPlanPersistenceService.copy(id, userId);
        log.info("[TripAPI] 复制行程 id={} copiedId={} userId={}", id, copiedId, userId);
        return Map.of("plan_id", copiedId);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        tripPlanPersistenceService.delete(id, LoginHelper.getUserId());
        log.info("[TripAPI] 删除行程 id={}", id);
        return Map.of("deleted", true);
    }

    @PostMapping("/{id}/chat")
    public TripChatResponse chat(@PathVariable long id, @RequestBody TripChatRequest request) {
        tripPlanPersistenceService.detail(id, LoginHelper.getUserId());
        log.info("[TripAPI] 行程聊天 id={} messageLength={}", id, request == null || request.message() == null ? 0 : request.message().length());
        return tripChatService.chat(id, request);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
