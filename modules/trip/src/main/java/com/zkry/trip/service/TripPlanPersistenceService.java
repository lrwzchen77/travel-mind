package com.zkry.trip.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripPlanPersistenceService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TripPlanPersistenceService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long save(long userId, TripPlanResponse response, TripRequest request) {
        if (response == null || response.data() == null) {
            throw new BizException("行程结果为空，无法保存。");
        }
        long tripId = nextId();
        TripPlan plan = withSources(response.data(), request.safeInspirationSources());
        TripPlanResponse prepared = new TripPlanResponse(response.success(), response.message(), response.plan_id(), plan, response.graph_data());
        jdbcTemplate.update("""
                INSERT INTO tm_trip_plan
                  (id, user_id, title, destination_city, start_date, end_date, travel_days, budget, total_cost, status,
                   summary, raw_plan_json)
                VALUES
                  (:id, :userId, :title, :destinationCity, :startDate, :endDate, :travelDays, :budget, :totalCost,
                   :status, :summary, :rawPlanJson)
                """,
            new MapSqlParameterSource()
                .addValue("id", tripId)
                .addValue("userId", userId)
                .addValue("title", title(plan))
                .addValue("destinationCity", plan.city())
                .addValue("startDate", plan.start_date())
                .addValue("endDate", plan.end_date())
                .addValue("travelDays", plan.days() == null ? request.safeTravelDays() : plan.days().size())
                .addValue("budget", BigDecimal.valueOf(plan.budget() == null ? 0 : plan.budget().total()))
                .addValue("totalCost", BigDecimal.valueOf(plan.budget() == null ? 0 : plan.budget().total()))
                .addValue("status", "generated")
                .addValue("summary", plan.overall_suggestions())
                .addValue("rawPlanJson", JsonUtils.toJsonString(withPlanId(prepared, String.valueOf(tripId)))));
        saveDays(tripId, plan);
        return tripId;
    }

    public TripPlanResponse detail(long tripId) {
        return detailQuery(tripId, null);
    }

    public TripPlanResponse detail(long tripId, long userId) {
        return detailQuery(tripId, userId);
    }

    private TripPlanResponse detailQuery(long tripId, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_trip_plan WHERE id = :id AND deleted = 0"
                + (userId == null ? "" : " AND user_id = :userId") + " LIMIT 1",
            userId == null ? Map.of("id", tripId) : Map.of("id", tripId, "userId", userId));
        if (rows.isEmpty()) {
            throw new BizException("行程不存在或已删除。");
        }
        Object json = rows.get(0).get("raw_plan_json");
        TripPlanResponse response = JsonUtils.parseObject(String.valueOf(json), TripPlanResponse.class);
        if (response == null || response.data() == null) {
            throw new BizException("行程详情数据不可解析。");
        }
        return response;
    }

    @Transactional
    public long copy(long tripId, long userId) {
        TripPlanResponse original = detail(tripId, userId);
        TripPlanResponse copied = withPlanId(original, "copy-" + tripId);
        return save(userId, copied, requestFromPlan(copied.data()));
    }

    @Transactional
    public void delete(long tripId) {
        int updated = jdbcTemplate.update(
            "UPDATE tm_trip_plan SET deleted = 1 WHERE id = :id AND deleted = 0",
            new MapSqlParameterSource().addValue("id", tripId));
        if (updated == 0) {
            throw new BizException("行程不存在或已删除。");
        }
    }

    @Transactional
    public void delete(long tripId, long userId) {
        int updated = jdbcTemplate.update(
            "UPDATE tm_trip_plan SET deleted = 1 WHERE id = :id AND user_id = :userId AND deleted = 0",
            new MapSqlParameterSource().addValue("id", tripId).addValue("userId", userId));
        if (updated == 0) {
            throw new BizException("行程不存在或无权操作。");
        }
    }

    private void saveDays(long tripId, TripPlan plan) {
        List<DayPlan> days = plan.days() == null ? List.of() : plan.days();
        for (DayPlan day : days) {
            long dayId = nextId();
            jdbcTemplate.update("""
                    INSERT INTO tm_trip_day (id, trip_plan_id, day_no, date, title, summary)
                    VALUES (:id, :tripPlanId, :dayNo, :date, :title, :summary)
                    """,
                new MapSqlParameterSource()
                    .addValue("id", dayId)
                    .addValue("tripPlanId", tripId)
                    .addValue("dayNo", day.day_index() == null ? days.indexOf(day) + 1 : day.day_index() + 1)
                    .addValue("date", day.date())
                    .addValue("title", "第" + ((day.day_index() == null ? days.indexOf(day) : day.day_index()) + 1) + "天 "
                        + safe(day.city()))
                    .addValue("summary", day.description()));
            saveDayItems(dayId, day, plan);
        }
    }

    private void saveDayItems(long dayId, DayPlan day, TripPlan plan) {
        int order = 1;
        if (day.hotel() != null) {
            insertItem(dayId, order++, "hotel", day.hotel().name(), day.hotel().address(), "", "",
                day.hotel().estimated_cost(), day.hotel().price_range());
        }
        if (!isBlank(day.transportation())) {
            insertItem(dayId, order++, "transportation", day.transportation(), day.city(), "", "", 40, day.transfer_info());
        }
        for (Attraction attraction : day.attractions() == null ? List.<Attraction>of() : day.attractions()) {
            insertItem(dayId, order++, "attraction", attraction.name(), attraction.address(), "", "",
                attraction.ticket_price(), attraction.description());
        }
        for (Meal meal : day.meals() == null ? List.<Meal>of() : day.meals()) {
            insertItem(dayId, order++, "meal", meal.name(), meal.address(), "", "", meal.estimated_cost(), meal.description());
        }
        if (plan.weather_info() != null && !plan.weather_info().isEmpty()) {
            String note = plan.weather_info().stream()
                .filter(weather -> day.date() != null && day.date().equals(weather.date()))
                .findFirst()
                .map(weather -> weather.day_weather() + " " + weather.day_temp() + "℃")
                .orElse("查看天气变化");
            insertItem(dayId, order, "weather_tip", "天气提示", day.city(), "", "", 0, note);
        }
    }

    private void insertItem(
        long dayId,
        int order,
        String type,
        String title,
        String location,
        String startTime,
        String endTime,
        Integer cost,
        String note
    ) {
        jdbcTemplate.update("""
                INSERT INTO tm_trip_item
                  (id, trip_day_id, item_order, item_type, title, location, start_time, end_time, cost, note)
                VALUES
                  (:id, :tripDayId, :itemOrder, :itemType, :title, :location, :startTime, :endTime, :cost, :note)
                """,
            new MapSqlParameterSource()
                .addValue("id", nextId())
                .addValue("tripDayId", dayId)
                .addValue("itemOrder", order)
                .addValue("itemType", type)
                .addValue("title", safe(title))
                .addValue("location", safe(location))
                .addValue("startTime", safe(startTime))
                .addValue("endTime", safe(endTime))
                .addValue("cost", BigDecimal.valueOf(cost == null ? 0 : cost))
                .addValue("note", safe(note)));
    }

    private TripPlanResponse withPlanId(TripPlanResponse response, String planId) {
        return new TripPlanResponse(response.success(), response.message(), planId, response.data(), response.graph_data());
    }

    private TripPlan withSources(TripPlan plan, List<com.zkry.trip.dto.InspirationSource> sources) {
        return new TripPlan(plan.city(), plan.cities(), plan.start_date(), plan.end_date(), plan.days(), plan.weather_info(),
            plan.overall_suggestions(), plan.budget(), sources == null ? List.of() : List.copyOf(sources),
            plan.public_data() == null ? List.of() : plan.public_data());
    }

    private TripRequest requestFromPlan(TripPlan plan) {
        return new TripRequest(plan.city(), null, plan.start_date(), plan.end_date(), plan.days() == null ? 1 : plan.days().size(),
            "", "", String.valueOf(plan.budget() == null ? 0 : plan.budget().total()), List.of(), "", "zh");
    }

    private String title(TripPlan plan) {
        return safe(plan.city()) + " " + safe(plan.start_date()) + " 旅行计划";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long nextId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
