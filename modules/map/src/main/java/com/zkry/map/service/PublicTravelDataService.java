package com.zkry.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.map.dto.MapPoint;
import com.zkry.map.dto.MapWeatherForecast;
import com.zkry.map.dto.PublicDataItem;
import com.zkry.map.dto.PublicTravelSnapshot;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 零 Key 演示数据：只访问固定公开端点，失败时返回空项，不阻断本地规划。 */
@Service
public class PublicTravelDataService {

    private static final Logger log = LoggerFactory.getLogger(PublicTravelDataService.class);
    private static final Duration WEATHER_TTL = Duration.ofMinutes(15);
    private static final Duration PLACES_TTL = Duration.ofDays(7);
    private static final Duration ROUTE_TTL = Duration.ofHours(24);
    private static final List<String> OVERPASS_ENDPOINTS = List.of(
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter"
    );
    private static final Map<String, City> CITIES = Map.of(
        "杭州", new City(120.1551, 30.2741, "HGH", "杭州萧山国际机场"),
        "北京", new City(116.4074, 39.9042, "PEK", "北京首都国际机场"),
        "成都", new City(104.0668, 30.5728, "TFU", "成都天府国际机场")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    // ponytail: 进程内缓存可能在并发刷新时重复请求；演示流量需要多实例时再换共享缓存。
    private final Map<String, Cached<WeatherResult>> weatherCache = new ConcurrentHashMap<>();
    private final Map<String, Cached<PlaceResult>> placesCache = new ConcurrentHashMap<>();
    private final Map<String, Cached<PublicDataItem>> routeCache = new ConcurrentHashMap<>();

    public PublicTravelSnapshot collect(String cityName) {
        String city = normalizeCity(cityName);
        City location = CITIES.get(city);
        if (location == null) {
            return PublicTravelSnapshot.empty();
        }

        List<PublicDataItem> items = new ArrayList<>();
        WeatherResult weather = cached(weatherCache, city, WEATHER_TTL);
        if (weather == null) {
            weather = loadWeather(city, location);
            if (weather != null) weatherCache.put(city, new Cached<>(Instant.now(), weather));
        }
        if (weather != null) items.add(weather.item());

        PlaceResult places = cached(placesCache, city, PLACES_TTL);
        if (places == null) {
            places = loadPlaces(city, location);
            if (places != null) placesCache.put(city, new Cached<>(Instant.now(), places));
        }
        if (places != null) {
            items.add(places.item());
            if (places.routePoints().size() >= 2) {
                String routeKey = city + ":" + places.routePoints().get(0).name() + ":" + places.routePoints().get(1).name();
                PublicDataItem route = cached(routeCache, routeKey, ROUTE_TTL);
                if (route == null) {
                    route = loadRoute(places.routePoints().get(0), places.routePoints().get(1));
                    if (route != null) routeCache.put(routeKey, new Cached<>(Instant.now(), route));
                }
                if (route != null) items.add(route);
            }
        }

        items.add(new PublicDataItem(
            location.airportName() + " · " + location.airportCode(),
            "项目内置的公共领域机场基础资料，不含航班时刻、准点信息、票价和舱位。",
            "OurAirports",
            "",
            "open_data",
            false,
            "https://ourairports.com/"
        ));
        return new PublicTravelSnapshot(weather == null ? List.of() : weather.forecasts(), items);
    }

    private WeatherResult loadWeather(String city, City location) {
        try {
            URI uri = URI.create("https://api.open-meteo.com/v1/forecast?latitude=" + location.latitude()
                + "&longitude=" + location.longitude()
                + "&current=temperature_2m,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=Asia%2FShanghai&forecast_days=16");
            JsonNode root = JsonUtils.getObjectMapper().readTree(get(uri));
            JsonNode current = root.path("current");
            if (current.isMissingNode() || !current.has("temperature_2m")) return null;
            String updatedAt = text(current.path("time"));
            if (updatedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) updatedAt += "+08:00";
            String weather = weatherLabel(current.path("weather_code").asInt(-1));
            String detail = "%s %.1f°C · %s · 风速 %.1f km/h".formatted(
                city, current.path("temperature_2m").asDouble(), weather, current.path("wind_speed_10m").asDouble());
            return new WeatherResult(parseForecasts(city, root.path("daily")), new PublicDataItem(
                "当前天气", detail, "Open-Meteo", updatedAt, "live", false, "https://open-meteo.com/"));
        } catch (Exception ex) {
            log.info("Open-Meteo 暂不可用 city={} reason={}", city, ex.getMessage());
            return null;
        }
    }

    private List<MapWeatherForecast> parseForecasts(String city, JsonNode daily) {
        JsonNode dates = daily.path("time");
        JsonNode codes = daily.path("weather_code");
        JsonNode maximums = daily.path("temperature_2m_max");
        JsonNode minimums = daily.path("temperature_2m_min");
        if (!dates.isArray()) return List.of();
        List<MapWeatherForecast> result = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            String label = weatherLabel(codes.path(i).asInt(-1));
            result.add(new MapWeatherForecast(
                dates.path(i).asText(), city, label, label,
                round(maximums.path(i)), round(minimums.path(i)), "", ""
            ));
        }
        return result;
    }

    private PlaceResult loadPlaces(String city, City location) {
        String query = "[out:json][timeout:8];("
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[tourism~\"attraction|museum|hotel|guest_house\"];"
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[amenity=restaurant];"
            + ");out center tags 30;";
        for (String endpoint : OVERPASS_ENDPOINTS) {
            try {
                URI uri = URI.create(endpoint + "?data=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                PlaceResult result = parsePlaces(city, JsonUtils.getObjectMapper().readTree(get(uri)));
                if (result != null) return result;
            } catch (Exception ex) {
                log.info("Overpass 节点暂不可用 endpoint={} city={} reason={}", endpoint, city, ex.getMessage());
            }
        }
        return null;
    }

    private PlaceResult parsePlaces(String city, JsonNode root) {
        JsonNode elements = root.path("elements");
        if (!elements.isArray()) return null;
        Map<String, List<String>> names = new LinkedHashMap<>();
        names.put("景区", new ArrayList<>());
        names.put("住宿", new ArrayList<>());
        names.put("餐饮", new ArrayList<>());
        List<NamedPoint> routePoints = new ArrayList<>();
        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            String name = firstNonBlank(text(tags.path("name:zh")), text(tags.path("name")));
            if (name.isBlank()) continue;
            String tourism = text(tags.path("tourism"));
            String kind = "restaurant".equals(text(tags.path("amenity"))) ? "餐饮"
                : ("hotel".equals(tourism) || "guest_house".equals(tourism)) ? "住宿" : "景区";
            List<String> group = names.get(kind);
            if (group.size() >= 3 || group.contains(name)) continue;
            group.add(name);
            MapPoint point = point(element);
            if ("景区".equals(kind) && point != null) routePoints.add(new NamedPoint(name, point));
        }
        String detail = names.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> entry.getKey() + "：" + String.join("、", entry.getValue()))
            .reduce((left, right) -> left + "；" + right)
            .orElse("");
        if (detail.isBlank()) return null;
        return new PlaceResult(routePoints, new PublicDataItem(
            city + "公开地点资料", detail + "。不含实时价格、库存和营业状态。",
            "OpenStreetMap", Instant.now().toString(), "open_data", false,
            "https://www.openstreetmap.org/copyright"
        ));
    }

    private PublicDataItem loadRoute(NamedPoint from, NamedPoint to) {
        try {
            URI uri = URI.create("https://router.project-osrm.org/route/v1/driving/"
                + from.point().longitude() + "," + from.point().latitude() + ";"
                + to.point().longitude() + "," + to.point().latitude() + "?overview=false");
            JsonNode route = JsonUtils.getObjectMapper().readTree(get(uri)).path("routes").path(0);
            if (!route.has("distance") || !route.has("duration")) return null;
            String detail = "%s → %s · 约 %.1f 公里 · 驾车约 %d 分钟；这是公开地点间的路线示例，不代表本行程安排，也不含实时拥堵。".formatted(
                from.name(), to.name(), route.path("distance").asDouble() / 1000,
                Math.max(1, Math.round(route.path("duration").asDouble() / 60)));
            return new PublicDataItem("公开景点间路线示例", detail, "OSRM / OpenStreetMap",
                Instant.now().toString(), "route_estimate", false, "https://project-osrm.org/");
        } catch (Exception ex) {
            log.info("OSRM 路线估算暂不可用 from={} to={} reason={}", from.name(), to.name(), ex.getMessage());
            return null;
        }
    }

    protected String get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(4))
            .header("User-Agent", "TravelMind-Demo/1.0 (public-data demonstration)")
            .GET()
            .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private <T> T cached(Map<String, Cached<T>> cache, String key, Duration ttl) {
        Cached<T> value = cache.get(key);
        return value != null && value.loadedAt().plus(ttl).isAfter(Instant.now()) ? value.value() : null;
    }

    private String normalizeCity(String value) {
        return value == null ? "" : value.trim().replaceFirst("市$", "");
    }

    private MapPoint point(JsonNode element) {
        JsonNode point = element.has("lat") ? element : element.path("center");
        if (!point.has("lat") || !point.has("lon")) return null;
        return new MapPoint(point.path("lon").asDouble(), point.path("lat").asDouble());
    }

    private Integer round(JsonNode value) {
        return value.isNumber() ? (int) Math.round(value.asDouble()) : null;
    }

    private String text(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? (second == null ? "" : second) : first;
    }

    private String weatherLabel(int code) {
        if (code == 0) return "晴";
        if (code <= 3) return "多云";
        if (code <= 48) return "有雾";
        if (code <= 57) return "毛毛雨";
        if (code <= 67) return "有雨";
        if (code <= 77) return "有雪";
        if (code <= 82) return "阵雨";
        if (code <= 86) return "阵雪";
        if (code <= 99) return "雷雨";
        return "天气待确认";
    }

    private record City(double longitude, double latitude, String airportCode, String airportName) {
    }

    private record Cached<T>(Instant loadedAt, T value) {
    }

    private record WeatherResult(List<MapWeatherForecast> forecasts, PublicDataItem item) {
    }

    private record PlaceResult(List<NamedPoint> routePoints, PublicDataItem item) {
    }

    private record NamedPoint(String name, MapPoint point) {
    }
}
