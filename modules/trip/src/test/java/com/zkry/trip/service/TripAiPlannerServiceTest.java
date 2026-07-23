package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zkry.ai.agent.TravelMindAgent;
import com.zkry.ai.service.AiAgentService;
import com.zkry.ai.service.AiStructuredOutputService;
import com.zkry.ai.service.PromptResourceService;
import com.zkry.common.core.constant.TravelDataSource;
import com.zkry.map.dto.MapPlanningContext;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.RouteIntent;
import com.zkry.trip.dto.RouteNode;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripAiPlannerServiceTest {

    private final AiAgentService agents = mock(AiAgentService.class);
    private final AiStructuredOutputService structured = mock(AiStructuredOutputService.class);
    private final PromptResourceService prompts = mock(PromptResourceService.class);
    private final TripAiPlannerService planner = new TripAiPlannerService(agents, structured, prompts);

    @BeforeEach
    void setUp() {
        when(prompts.load(any())).thenReturn("system");
        when(prompts.render(any(), anyMap())).thenReturn("prompt");
        when(structured.format(any(Class.class))).thenReturn("{}");
    }

    @Test
    void keepsParsedPlannerResultWhenAiReviewRejectsIt() {
        TripPlan aiPlan = aiPlan();
        stubPlanner(aiPlan);
        when(structured.callForObject(
            eq(TravelMindAgent.TRIP_REVIEW), eq(TripAiPlannerService.ReviewResult.class),
            anyString(), anyString(), eq("plan-1-review")
        )).thenReturn(Optional.of(new TripAiPlannerService.ReviewResult(
            false, List.of("缺少早餐"), List.of("补充早餐")
        )));

        var result = planner.plan("plan-1", request(), emptyMapContext());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().data().overall_suggestions()).isEqualTo("真实 AI 行程");
        assertThat(result.orElseThrow().data().route_intent()).isEqualTo(request().route_intent());
        assertThat(result.orElseThrow().data().days()).extracting(DayPlan::day_index).containsExactly(0, 1);
    }

    @Test
    void keepsParsedPlannerResultWhenAiReviewReturnsNothing() {
        stubPlanner(aiPlan());
        when(structured.callForObject(
            eq(TravelMindAgent.TRIP_REVIEW), eq(TripAiPlannerService.ReviewResult.class),
            anyString(), anyString(), eq("plan-1-review")
        )).thenReturn(Optional.empty());

        assertThat(planner.plan("plan-1", request(), emptyMapContext())).isPresent();
    }

    @Test
    void stillReturnsEmptyWhenPlannerHasNoParsedResult() {
        when(structured.callForObject(
            eq(TravelMindAgent.TRIP_PLANNER), eq(TripPlan.class),
            anyString(), anyString(), eq("plan-1-planner")
        )).thenReturn(Optional.empty());

        assertThat(planner.plan("plan-1", request(), emptyMapContext())).isEmpty();
    }

    private void stubPlanner(TripPlan plan) {
        when(structured.callForObject(
            eq(TravelMindAgent.TRIP_PLANNER), eq(TripPlan.class),
            anyString(), anyString(), eq("plan-1-planner")
        )).thenReturn(Optional.of(plan));
    }

    private TripPlan aiPlan() {
        return new TripPlan(
            "杭州", List.of("杭州"), "2026-07-24", "2026-07-25", List.of(
                new DayPlan("2026-07-24", 1, "杭州", false, "", "西湖", "公交", "酒店", null, List.of(), List.of()),
                new DayPlan("2026-07-25", 2, "杭州", false, "", "龙井路", "公交", "酒店", null, List.of(), List.of())
            ), List.of(),
            "真实 AI 行程", new Budget(0, 700, 1500, 300, 0, 2500)
        );
    }

    private TripRequest request() {
        RouteIntent route = new RouteIntent("杭州", "soft_order", List.of(
            new RouteNode(1, "poi", "west-lake", "西湖", 120.1485, 30.242, "attraction", "傍晚看日落", List.of("必去", "拍照")),
            new RouteNode(2, "free_point", null, "龙井路慢游段", 120.1152, 30.2288, null, "少走一点", List.of("慢游"))
        ));
        return new TripRequest(
            "杭州", null, "2026-07-24", "2026-07-25", 2,
            "公共交通", "亲子酒店", "4200", List.of("湖景", "轻松"),
            "2 位成人、1 位儿童；中午休息一小时", "zh-CN", List.of(), List.of(), route
        );
    }

    private MapPlanningContext emptyMapContext() {
        return MapPlanningContext.empty(TravelDataSource.NONE, "test");
    }
}
