package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripPlanReviewerTest {

    private final TripPlanReviewer reviewer = new TripPlanReviewer();

    @Test
    void acceptsCompletePlanMatchingRequestDays() {
        TripPlanReviewer.ReviewOutcome outcome = reviewer.review(completePlan(), request(2));

        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.issues()).isEmpty();
    }

    @Test
    void rejectsPlanWithoutDailySchedule() {
        TripPlan invalid = new TripPlan("Hangzhou", List.of("Hangzhou"), "2026-08-01", "2026-08-02", List.of(),
            List.of(), "No daily plan", new Budget(0, 0, 0, 0, 0, 0));

        TripPlanReviewer.ReviewOutcome outcome = reviewer.review(invalid, request(2));

        assertThat(outcome.passed()).isFalse();
        assertThat(outcome.issues()).contains("行程天数与请求不一致");
    }

    private TripRequest request(int days) {
        return new TripRequest("Hangzhou", null, "2026-08-01", "2026-08-02", days, "公共交通", "舒适型酒店", "3000",
            List.of("湖景", "美食"), "节奏轻松", "zh");
    }

    private TripPlan completePlan() {
        Hotel hotel = new Hotel("West Lake Hotel", "Hangzhou", null, "500-700", "4.7", "near lake", "舒适型", 600);
        List<DayPlan> days = List.of(
            new DayPlan("2026-08-01", 0, "Hangzhou", false, "", "西湖慢行", "公共交通", "舒适型酒店", hotel,
                List.of(new Attraction("West Lake", "Hangzhou", null, 180, "湖景步行", "nature", 4.9, "", 0)),
                List.of(new Meal("午餐", "湖滨餐厅", "Hangzhou", null, "本地菜", 80))),
            new DayPlan("2026-08-02", 1, "Hangzhou", false, "", "寺庙文化", "公共交通", "舒适型酒店", hotel,
                List.of(new Attraction("Lingyin Temple", "Hangzhou", null, 120, "文化体验", "culture", 4.7, "", 45)),
                List.of(new Meal("晚餐", "灵隐素斋", "Hangzhou", null, "素食", 70)))
        );
        return new TripPlan("Hangzhou", List.of("Hangzhou"), "2026-08-01", "2026-08-02", days,
            List.of(new WeatherInfo("2026-08-01", "Hangzhou", "晴", "多云", 31, 24, "东风", "3级")),
            "注意防晒，保留弹性时间。", new Budget(45, 1200, 150, 80, 0, 1475));
    }
}
