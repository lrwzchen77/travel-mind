package com.zkry.trip.service;

import com.zkry.ai.agent.TravelMindAgent;
import com.zkry.ai.prompt.TravelMindPrompt;
import com.zkry.ai.prompt.TravelMindPromptVariable;
import com.zkry.ai.service.AiAgentService;
import com.zkry.ai.service.PromptResourceService;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.TripChatRequest;
import com.zkry.trip.dto.TripChatResponse;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TripChatService {

    private final TripPlanPersistenceService persistenceService;
    private final AiAgentService aiAgentService;
    private final PromptResourceService promptResourceService;

    public TripChatService(
        TripPlanPersistenceService persistenceService,
        AiAgentService aiAgentService,
        PromptResourceService promptResourceService
    ) {
        this.persistenceService = persistenceService;
        this.aiAgentService = aiAgentService;
        this.promptResourceService = promptResourceService;
    }

    public TripChatResponse chat(long tripId, TripChatRequest request) {
        TripPlanResponse saved = persistenceService.detail(tripId);
        TripPlan plan = saved.data();
        String message = request == null || request.message() == null ? "" : request.message();
        if (aiAgentService.isAvailable()) {
            String prompt = promptResourceService.render(TravelMindPrompt.CHAT_USER, Map.of(
                TravelMindPromptVariable.TRIP_PLAN, JsonUtils.toJsonString(plan),
                TravelMindPromptVariable.MESSAGE, message
            ));
            return aiAgentService.call(
                    TravelMindAgent.TRIP_CHAT,
                    promptResourceService.load(TravelMindPrompt.CHAT_SYSTEM),
                    prompt,
                    "trip-chat-" + tripId
                )
                .map(reply -> new TripChatResponse(true, reply))
                .orElseGet(() -> fallback(plan, message));
        }
        return fallback(plan, message);
    }

    private TripChatResponse fallback(TripPlan plan, String message) {
        String city = safe(plan.city());
        int days = plan.days() == null ? 0 : plan.days().size();
        int budget = plan.budget() == null || plan.budget().total() == null ? 0 : plan.budget().total();
        String firstDay = firstDaySummary(plan.days());
        String reply = "已根据已保存的" + city + "行程回答：共" + days + "天，预计总预算" + budget
            + "元。首日安排：" + firstDay + "。你的问题是：" + safe(message)
            + "。如需优化，建议优先调整每日景点数量、餐饮预算和交通方式。";
        return new TripChatResponse(true, reply);
    }

    private String firstDaySummary(List<DayPlan> days) {
        if (days == null || days.isEmpty()) {
            return "暂无每日安排";
        }
        DayPlan day = days.get(0);
        String attractions = day.attractions() == null || day.attractions().isEmpty()
            ? "暂无景点"
            : day.attractions().stream().map(item -> safe(item.name())).limit(3).toList().toString();
        return safe(day.date()) + " " + attractions;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未填写" : value;
    }
}
