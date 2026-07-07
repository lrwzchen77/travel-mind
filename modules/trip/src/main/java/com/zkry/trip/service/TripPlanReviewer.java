package com.zkry.trip.service;

import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TripPlanReviewer {

    public ReviewOutcome review(TripPlan plan, TripRequest request) {
        List<String> issues = new ArrayList<>();
        if (plan == null) {
            return new ReviewOutcome(false, List.of("行程结果为空"));
        }
        if (isBlank(plan.city()) && (plan.cities() == null || plan.cities().isEmpty())) {
            issues.add("缺少目的地城市");
        }
        List<DayPlan> days = plan.days() == null ? List.of() : plan.days();
        int expectedDays = request == null ? days.size() : request.safeTravelDays();
        if (days.size() != expectedDays) {
            issues.add("行程天数与请求不一致");
        }
        if (plan.budget() == null || plan.budget().total() == null || plan.budget().total() <= 0) {
            issues.add("缺少有效预算");
        }
        if (plan.weather_info() == null || plan.weather_info().isEmpty()) {
            issues.add("缺少天气提示");
        }
        for (DayPlan day : days) {
            int displayDay = day.day_index() == null ? days.indexOf(day) + 1 : day.day_index() + 1;
            if (isBlank(day.date())) {
                issues.add("第" + displayDay + "天缺少日期");
            }
            if (isBlank(day.transportation())) {
                issues.add("第" + displayDay + "天缺少交通安排");
            }
            if (day.hotel() == null) {
                issues.add("第" + displayDay + "天缺少酒店安排");
            }
            if (day.attractions() == null || day.attractions().isEmpty()) {
                issues.add("第" + displayDay + "天缺少景点安排");
            }
            if (day.meals() == null || day.meals().isEmpty()) {
                issues.add("第" + displayDay + "天缺少餐饮安排");
            }
        }
        return new ReviewOutcome(issues.isEmpty(), List.copyOf(issues));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ReviewOutcome(boolean passed, List<String> issues) {
    }
}
