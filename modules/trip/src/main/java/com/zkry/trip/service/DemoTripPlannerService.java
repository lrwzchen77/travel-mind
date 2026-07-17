package com.zkry.trip.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.map.dto.MapWeatherForecast;
import com.zkry.map.dto.PublicDataItem;
import com.zkry.map.dto.PublicTravelSnapshot;
import com.zkry.map.service.PublicTravelDataService;
import com.zkry.trip.dto.Attraction;
import com.zkry.trip.dto.Budget;
import com.zkry.trip.dto.CityStay;
import com.zkry.trip.dto.DayPlan;
import com.zkry.trip.dto.Hotel;
import com.zkry.trip.dto.Meal;
import com.zkry.trip.dto.TripPlan;
import com.zkry.trip.dto.TripPlanResponse;
import com.zkry.trip.dto.TripRequest;
import com.zkry.trip.dto.WeatherInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DemoTripPlannerService {

    private static final Logger log = LoggerFactory.getLogger(DemoTripPlannerService.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TripPlanReviewer reviewer;
    private final PublicTravelDataService publicTravelDataService;

    public DemoTripPlannerService(
        NamedParameterJdbcTemplate jdbcTemplate,
        TripPlanReviewer reviewer,
        PublicTravelDataService publicTravelDataService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewer = reviewer;
        this.publicTravelDataService = publicTravelDataService;
    }

    public TripPlanResponse plan(String planId, TripRequest request) {
        List<CityStay> cityStays = request.normalizedCities();
        if (cityStays.isEmpty()) {
            throw new BizException("请至少填写一个目的地城市。");
        }
        List<String> cityNames = cityStays.stream().map(CityStay::city).toList();
        String primaryCity = cityNames.get(0);
        Map<String, Object> city = findCity(primaryCity);
        Long cityId = numberAsLong(city.get("id"));
        List<Map<String, Object>> attractions = queryAttractions(cityId, primaryCity);
        List<Map<String, Object>> hotels = queryHotels(cityId, primaryCity);
        List<Map<String, Object>> restaurants = queryRestaurants(cityId, primaryCity);
        List<Map<String, Object>> notes = queryNotes(cityId);
        PublicTravelSnapshot publicData = collectPublicData(primaryCity);

        int daysCount = request.safeTravelDays();
        LocalDate startDate = parseDate(request.start_date());
        List<DayPlan> days = new ArrayList<>();
        List<WeatherInfo> weather = new ArrayList<>();
        int attractionCost = 0;
        int hotelCost = 0;
        int mealCost = 0;
        int transportCost = 0;
        for (int i = 0; i < daysCount; i++) {
            Map<String, Object> attractionRow = pick(attractions, i);
            Map<String, Object> secondAttractionRow = pick(attractions, i + 1);
            Map<String, Object> hotelRow = pick(hotels, i);
            Map<String, Object> restaurantRow = pick(restaurants, i);
            String date = startDate.plusDays(i).toString();
            Hotel hotel = toHotel(hotelRow);
            List<Attraction> dayAttractions = distinctAttractions(attractionRow, secondAttractionRow);
            List<Meal> meals = List.of(toMeal("午餐", restaurantRow), toMeal("晚餐", restaurantRow));
            attractionCost += dayAttractions.stream().mapToInt(item -> item.ticket_price() == null ? 0 : item.ticket_price()).sum();
            hotelCost += hotel.estimated_cost() == null ? 0 : hotel.estimated_cost();
            mealCost += meals.stream().mapToInt(item -> item.estimated_cost() == null ? 0 : item.estimated_cost()).sum();
            transportCost += 40;
            weather.add(weatherFor(date, primaryCity, publicData.safeWeather()));
            days.add(new DayPlan(
                date,
                i,
                primaryCity,
                false,
                "",
                dailyDescription(primaryCity, i, notes),
                request.safeTransportation(),
                request.safeAccommodation(),
                hotel,
                dayAttractions,
                meals
            ));
        }
        Budget budget = new Budget(attractionCost, hotelCost, mealCost, transportCost, 0,
            attractionCost + hotelCost + mealCost + transportCost);
        List<PublicDataItem> publicItems = new ArrayList<>(publicData.safeItems());
        publicItems.add(new PublicDataItem(
            "行程内演示预算约 ¥" + budget.total(),
            "仅按当前本地演示资源估算，未接入同行人数计价及铁路、航班、酒店、门票的实时价格和库存。",
            "Travel Mind 本地演示资料",
            "",
            "demo_reference",
            false,
            ""
        ));
        publicItems.add(new PublicDataItem(
            "铁路票价与余票请到 12306 核验",
            "平台不查询、不缓存铁路余票，也不提供购票或退改签服务。",
            "铁路 12306",
            "",
            "demo_reference",
            false,
            "https://www.12306.cn/index/"
        ));
        TripPlan plan = new TripPlan(
            primaryCity,
            cityNames,
            startDate.toString(),
            startDate.plusDays(daysCount - 1L).toString(),
            days,
            weather,
            "当前为演示规划，预算未接入实时交通、价格和库存，请勿作为预订依据。",
            budget,
            List.of(),
            publicItems
        );
        TripPlanReviewer.ReviewOutcome outcome = reviewer.review(plan, request);
        if (!outcome.passed()) {
            throw new BizException("Demo 行程质检未通过：" + String.join("；", outcome.issues()));
        }
        return TripPlanResponseFactory.fromPlan(planId, plan);
    }

    private PublicTravelSnapshot collectPublicData(String city) {
        try {
            return publicTravelDataService.collect(city);
        } catch (Exception ex) {
            log.info("免费公开数据暂不可用，继续生成本地演示行程 city={} reason={}", city, ex.getMessage());
            return PublicTravelSnapshot.empty();
        }
    }

    private WeatherInfo weatherFor(String date, String city, List<MapWeatherForecast> forecasts) {
        return forecasts.stream()
            .filter(item -> date.equals(item.date()))
            .findFirst()
            .map(item -> new WeatherInfo(date, city, item.dayWeather(), item.nightWeather(), item.dayTemp(),
                item.nightTemp(), item.windDirection(), item.windPower()))
            .orElseGet(() -> new WeatherInfo(date, city, "出发前确认", "出发前确认", null, null, "", ""));
    }

    private Map<String, Object> findCity(String cityName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_city WHERE deleted = 0 AND name LIKE :keyword ORDER BY popularity DESC, id DESC LIMIT 1",
            Map.of("keyword", "%" + cityName + "%"));
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        return new LinkedHashMap<>(Map.of("id", 0L, "name", cityName, "province", ""));
    }

    private List<Map<String, Object>> queryAttractions(Long cityId, String cityName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_attraction WHERE deleted = 0 AND city_id = :cityId "
                + "ORDER BY rating DESC, id ASC LIMIT 12",
            Map.of("cityId", cityId == null ? 0L : cityId));
        if (!rows.isEmpty()) {
            return rows;
        }
        return List.of(Map.of("name", cityName + "城市漫步", "address", cityName, "category", "city_walk",
            "rating", BigDecimal.valueOf(4.5), "price", BigDecimal.ZERO, "description", "本地资源库暂无景点，安排轻量城市漫步。"));
    }

    private List<Map<String, Object>> queryHotels(Long cityId, String cityName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_hotel WHERE deleted = 0 AND city_id = :cityId "
                + "ORDER BY rating DESC, id ASC LIMIT 6",
            Map.of("cityId", cityId == null ? 0L : cityId));
        if (!rows.isEmpty()) {
            return rows;
        }
        return List.of(Map.of("name", cityName + "舒适酒店", "address", cityName, "rating", BigDecimal.valueOf(4.3),
            "price_range", "400-600", "category", "hotel"));
    }

    private List<Map<String, Object>> queryRestaurants(Long cityId, String cityName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM tm_restaurant WHERE deleted = 0 AND city_id = :cityId "
                + "ORDER BY rating DESC, id ASC LIMIT 8",
            Map.of("cityId", cityId == null ? 0L : cityId));
        if (!rows.isEmpty()) {
            return rows;
        }
        return List.of(Map.of("name", cityName + "本地餐厅", "address", cityName, "average_cost", BigDecimal.valueOf(80),
            "cuisine", "local", "description", "本地风味餐。"));
    }

    private List<Map<String, Object>> queryNotes(Long cityId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM tm_travel_note WHERE deleted = 0 AND city_id = :cityId "
                + "ORDER BY update_time DESC LIMIT 5",
            Map.of("cityId", cityId == null ? 0L : cityId));
    }

    private List<Attraction> distinctAttractions(Map<String, Object> first, Map<String, Object> second) {
        Attraction one = toAttraction(first);
        Attraction two = toAttraction(second);
        if (one.name().equals(two.name())) {
            return List.of(one);
        }
        return List.of(one, two);
    }

    private Attraction toAttraction(Map<String, Object> row) {
        return new Attraction(
            string(row.get("name")),
            string(row.get("address")),
            null,
            120,
            string(row.get("description")),
            string(row.get("category")),
            numberAsDouble(row.get("rating")),
            string(row.get("image_url")),
            numberAsInt(row.get("price"))
        );
    }

    private Hotel toHotel(Map<String, Object> row) {
        return new Hotel(
            string(row.get("name")),
            string(row.get("address")),
            null,
            string(row.get("price_range")),
            string(row.get("rating")),
            "交通便利",
            string(row.get("category")),
            priceRangeAverage(string(row.get("price_range")))
        );
    }

    private Meal toMeal(String type, Map<String, Object> row) {
        return new Meal(
            type,
            string(row.get("name")),
            string(row.get("address")),
            null,
            string(row.get("description")),
            Math.max(60, numberAsInt(row.get("average_cost")))
        );
    }

    private String dailyDescription(String city, int dayIndex, List<Map<String, Object>> notes) {
        if (notes == null || notes.isEmpty()) {
            return city + "第" + (dayIndex + 1) + "天资源库推荐路线。";
        }
        Map<String, Object> note = pick(notes, dayIndex);
        return string(note.get("title")) + "：" + string(note.get("content"));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now().plusDays(14);
        }
        return LocalDate.parse(value);
    }

    private Map<String, Object> pick(List<Map<String, Object>> rows, int index) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.get(Math.floorMod(index, rows.size()));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Double numberAsDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int numberAsInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return new BigDecimal(String.valueOf(value)).intValue();
    }

    private int priceRangeAverage(String priceRange) {
        if (priceRange == null || priceRange.isBlank()) {
            return 500;
        }
        String[] parts = priceRange.replace("¥", "").split("-");
        if (parts.length == 2) {
            try {
                return (Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim())) / 2;
            } catch (NumberFormatException ignored) {
                return 500;
            }
        }
        return 500;
    }
}
