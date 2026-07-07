package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zkry.ai.service.AiAgentService;
import com.zkry.ai.service.PromptResourceService;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripChatRequest;
import com.zkry.trip.dto.TripChatResponse;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripChatServiceTest {

    @Test
    void repliesFromSavedPlanWhenAiIsUnavailable() {
        TripPlanPersistenceService persistenceService = org.mockito.Mockito.mock(TripPlanPersistenceService.class);
        AiAgentService aiAgentService = org.mockito.Mockito.mock(AiAgentService.class);
        PromptResourceService promptResourceService = org.mockito.Mockito.mock(PromptResourceService.class);
        when(persistenceService.detail(9001L)).thenReturn(response());
        when(aiAgentService.isAvailable()).thenReturn(false);
        TripChatService service = new TripChatService(persistenceService, aiAgentService, promptResourceService);

        TripChatResponse response = service.chat(9001L, new TripChatRequest("预算是多少？", null, List.of()));

        assertThat(response.success()).isTrue();
        assertThat(response.reply()).contains("Hangzhou");
        assertThat(response.reply()).contains("720");
    }

    private TripPlanResponse response() {
        Hotel hotel = new Hotel("West Lake Hotel", "Hangzhou", null, "500-700", "4.7", "near lake", "舒适型", 600);
        TripPlan plan = new TripPlan("Hangzhou", List.of("Hangzhou"), "2026-08-01", "2026-08-01", List.of(
            new DayPlan("2026-08-01", 0, "Hangzhou", false, "", "西湖慢行", "公共交通", "舒适型酒店", hotel,
                List.of(new Attraction("West Lake", "Hangzhou", null, 180, "湖景步行", "nature", 4.9, "", 0)),
                List.of(new Meal("午餐", "湖滨餐厅", "Hangzhou", null, "本地菜", 80)))
        ), List.of(), "注意防晒。", new Budget(0, 600, 80, 40, 0, 720));
        return TripPlanResponseFactory.fromPlan("9001", plan);
    }
}
