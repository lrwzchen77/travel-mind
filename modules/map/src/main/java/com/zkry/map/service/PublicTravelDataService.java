package com.zkry.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.map.dto.MapPoint;
import com.zkry.map.dto.MapPoi;
import com.zkry.map.dto.MapWeatherForecast;
import com.zkry.map.dto.PublicDataItem;
import com.zkry.map.dto.PublicTravelMapSnapshot;
import com.zkry.map.dto.PublicTravelSnapshot;
import com.zkry.map.util.MapCoordinates;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/** 高德优先、开放地图回退的旅行地图数据；外部失败不阻断本地规划。 */
@Service
public class PublicTravelDataService {

    private static final Logger log = LoggerFactory.getLogger(PublicTravelDataService.class);
    private static final Duration WEATHER_TTL = Duration.ofMinutes(15);
    private static final Duration PLACES_TTL = Duration.ofDays(7);
    private static final Duration ROUTE_TTL = Duration.ofHours(24);
    private static final int AMAP_PAGE_SIZE = 25;
    private static final List<String> OVERPASS_ENDPOINTS = List.of(
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter"
    );
    private static final Map<String, City> CITIES = Map.of(
        "杭州", new City(120.1551, 30.2741, "HGH", "杭州萧山国际机场", 120.4344, 30.2295),
        "北京", new City(116.4074, 39.9042, "PEK", "北京首都国际机场", 116.5975, 40.0799),
        "成都", new City(104.0668, 30.5728, "TFU", "成都天府国际机场", 104.441, 30.3125)
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private final AmapMapContextService amapMapContextService;
    // ponytail: 进程内缓存可能在并发刷新时重复请求；演示流量需要多实例时再换共享缓存。
    private final Map<String, Cached<WeatherResult>> weatherCache = new ConcurrentHashMap<>();
    private final Map<String, Cached<PlaceResult>> placesCache = new ConcurrentHashMap<>();
    private final Map<String, Cached<RouteResult>> routeCache = new ConcurrentHashMap<>();

    public PublicTravelDataService() {
        this(null);
    }

    @Autowired
    public PublicTravelDataService(AmapMapContextService amapMapContextService) {
        this.amapMapContextService = amapMapContextService;
    }

    public PublicTravelSnapshot collect(String cityName) {
        String city = normalizeCity(cityName);
        City location = CITIES.get(city);
        if (location == null) {
            return PublicTravelSnapshot.empty();
        }

        List<PublicDataItem> items = new ArrayList<>();
        WeatherResult weather = weather(city, location);
        if (weather != null) items.add(weather.item());

        PlaceResult places = places(city, location);
        if (places != null) {
            items.add(places.item());
            RouteResult route = route(city, places);
            if (route != null) items.add(route.item());
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

    public PublicTravelMapSnapshot collectMap(String cityName) {
        return collectMap(cityName, null, null);
    }

    public PublicTravelMapSnapshot collectMap(String cityName, Double longitude, Double latitude) {
        String city = normalizeCity(cityName);
        City location = CITIES.get(city);
        if (location == null && validCoordinates(longitude, latitude)) {
            location = new City(longitude, latitude, "", "", longitude, latitude);
        }
        if (location == null) return PublicTravelMapSnapshot.empty(city);

        WeatherResult weather = weather(city, location);
        PlaceResult places = places(city, location);
        RouteResult route = places == null ? null : route(city, places);
        return new PublicTravelMapSnapshot(
            city,
            weather == null ? null : new PublicTravelMapSnapshot.Weather(
                weather.temperature(), weather.condition(), weather.windSpeed(), weather.updatedAt(),
                weather.forecasts(), "Open-Meteo"),
            places == null ? List.of() : places.places(),
            route == null ? null : route.route(),
            location.airportCode().isBlank() ? null : new PublicTravelMapSnapshot.Airport(
                location.airportCode(), location.airportName(), location.airportLongitude(), location.airportLatitude(),
                "OurAirports", "公共领域机场基础资料，不含航班时刻、准点信息、票价和舱位。"),
            new PublicTravelMapSnapshot.RailwayCheck(
                "https://www.12306.cn/index/", "仅跳转 12306 官网核验，不提供车次、余票或票价。"),
            new PublicTravelMapSnapshot.Availability(
                weather == null ? "unavailable" : "available",
                places == null ? "unavailable" : "available",
                route == null ? "unavailable" : "available")
        );
    }

    private WeatherResult weather(String city, City location) {
        String key = locationKey(city, location);
        WeatherResult result = cached(weatherCache, key, WEATHER_TTL);
        if (result == null) {
            result = loadWeather(city, location);
            if (result != null) weatherCache.put(key, new Cached<>(Instant.now(), result));
        }
        return result;
    }

    private PlaceResult places(String city, City location) {
        String key = locationKey(city, location);
        PlaceResult result = cached(placesCache, key, PLACES_TTL);
        if (result == null) {
            result = loadAmapPlaces(city, location);
            if (!hasEnoughPlaces(result)) result = mergePlaces(result, loadPlaces(city, location));
            if (result != null) placesCache.put(key, new Cached<>(Instant.now(), result));
        }
        return result;
    }

    private boolean hasEnoughPlaces(PlaceResult result) {
        if (result == null) return false;
        return Stream.of("attraction", "hotel", "restaurant")
            .allMatch(kind -> result.places().stream().filter(place -> kind.equals(place.kind())).count() >= 10);
    }

    private PlaceResult mergePlaces(PlaceResult primary, PlaceResult fallback) {
        if (primary == null) return fallback;
        if (fallback == null) return primary;
        Map<String, List<PublicTravelMapSnapshot.Place>> groups = new LinkedHashMap<>();
        groups.put("attraction", new ArrayList<>());
        groups.put("hotel", new ArrayList<>());
        groups.put("restaurant", new ArrayList<>());
        Set<String> names = new HashSet<>();
        Stream.concat(primary.places().stream(), fallback.places().stream()).forEach(place -> {
            String name = normalizePlaceName(place.name());
            if (names.add(name)) groups.computeIfAbsent(place.kind(), ignored -> new ArrayList<>()).add(place);
        });
        List<PublicTravelMapSnapshot.Place> places = groups.values().stream()
            .flatMap(group -> group.stream().sorted(Comparator.comparingDouble(PublicTravelMapSnapshot.Place::distance_km)))
            .toList();
        return new PlaceResult(places, primary.item());
    }

    private RouteResult route(String city, PlaceResult places) {
        List<PublicTravelMapSnapshot.Place> attractions = places.places().stream()
            .filter(place -> "attraction".equals(place.kind())).toList();
        if (attractions.size() < 2) return null;
        String routeKey = city + ":" + attractions.get(0).name() + ":" + attractions.get(1).name();
        RouteResult result = cached(routeCache, routeKey, ROUTE_TTL);
        if (result == null) {
            result = loadRoute(attractions.get(0), attractions.get(1));
            if (result != null) routeCache.put(routeKey, new Cached<>(Instant.now(), result));
        }
        return result;
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
            return new WeatherResult(
                parseForecasts(city, root.path("daily")),
                current.path("temperature_2m").asDouble(), weather,
                current.path("wind_speed_10m").asDouble(), updatedAt,
                new PublicDataItem("当前天气", detail, "Open-Meteo", updatedAt, "live", false, "https://open-meteo.com/"));
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

    private PlaceResult loadAmapPlaces(String city, City location) {
        if (amapMapContextService == null || !amapMapContextService.ready()) return null;
        MapPoint gcjCenter = MapCoordinates.wgs84ToGcj02(new MapPoint(location.longitude(), location.latitude()));
        CompletableFuture<List<PublicTravelMapSnapshot.Place>> attractions = CompletableFuture.supplyAsync(() ->
            amapPlaces(city, location, gcjCenter, "attraction", "", "110000"));
        CompletableFuture<List<PublicTravelMapSnapshot.Place>> hotels = CompletableFuture.supplyAsync(() ->
            amapPlaces(city, location, gcjCenter, "hotel", "", "100000"));
        CompletableFuture<List<PublicTravelMapSnapshot.Place>> restaurants = CompletableFuture.supplyAsync(() ->
            amapPlaces(city, location, gcjCenter, "restaurant", "", "050000"));
        List<PublicTravelMapSnapshot.Place> places = Stream.of(attractions, hotels, restaurants)
            .flatMap(future -> future.join().stream()).toList();
        if (places.isEmpty()) return null;
        String updatedAt = Instant.now().toString();
        String detail = Stream.of("attraction", "hotel", "restaurant")
            .map(kind -> places.stream().filter(place -> kind.equals(place.kind())).map(PublicTravelMapSnapshot.Place::name)
                .reduce((left, right) -> left + "、" + right)
                .map(names -> kindLabel(kind) + "：" + names).orElse(""))
            .filter(value -> !value.isBlank()).reduce((left, right) -> left + "；" + right).orElse("");
        return new PlaceResult(places, new PublicDataItem(
            city + "高德周边地点", detail + "。营业时间、评分和消费信息以商户现场为准。",
            "高德地图", updatedAt, "live", false,
            "https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch"));
    }

    private List<PublicTravelMapSnapshot.Place> amapPlaces(
        String city, City location, MapPoint gcjCenter, String kind, String keywords, String types
    ) {
        try {
            String updatedAt = Instant.now().toString();
            int pages = "attraction".equals(kind) ? 3 : 1;
            List<MapPoi> candidates = new ArrayList<>(
                amapMapContextService.searchAround(gcjCenter, city, keywords, types, AMAP_PAGE_SIZE, pages));
            if ("attraction".equals(kind)) {
                candidates.addAll(amapMapContextService.searchPois(city, city + " 热门景点", AMAP_PAGE_SIZE));
            }
            return candidates
                .stream().filter(poi -> poi.location() != null && poi.location().available())
                .map(poi -> new PublicTravelMapSnapshot.Place(
                    "amap-" + firstNonBlank(poi.id(), normalizePlaceName(poi.name()) + '-' + poi.location().longitude()
                        + '-' + poi.location().latitude()),
                    poi.name(), kind, poi.location().longitude(), poi.location().latitude(), poi.address(),
                    amapCategory(poi, kind), firstNonBlank(poi.openTimeWeek(), poi.openTimeToday()),
                    distanceKm(location, poi.location()), parseDouble(poi.rating()), parseDouble(poi.cost()),
                    poi.photoUrl(), poi.tag(), 0, "", "高德地图", updatedAt
                )).sorted(Comparator.comparingDouble(PublicTravelMapSnapshot.Place::distance_km))
                .toList();
        } catch (Exception ex) {
            log.info("高德周边地点暂不可用 city={} kind={} reason={}", city, kind, ex.getMessage());
            return List.of();
        }
    }

    private String amapCategory(MapPoi poi, String kind) {
        if (poi.type() != null && !poi.type().isBlank()) {
            String[] values = poi.type().split("[;；]");
            for (int i = values.length - 1; i >= 0; i--) if (!values[i].isBlank()) return values[i].trim();
        }
        return kindLabel(kind);
    }

    private String kindLabel(String kind) {
        return "hotel".equals(kind) ? "住宿" : "restaurant".equals(kind) ? "餐饮" : "景点";
    }

    private PlaceResult loadPlaces(String city, City location) {
        String query = "[out:json][timeout:8];("
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[tourism~\"attraction|museum|gallery|viewpoint|zoo|aquarium|theme_park\"];"
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[historic];"
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[leisure~\"park|garden|nature_reserve\"];"
            + ")->.attractions;.attractions out center tags 120;("
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[tourism~\"hotel|guest_house|hostel|motel\"];"
            + ")->.hotels;.hotels out center tags 120;("
            + "nwr(around:12000," + location.latitude() + "," + location.longitude() + ")[amenity~\"restaurant|cafe|fast_food|food_court\"];"
            + ")->.restaurants;.restaurants out center tags 120;";
        for (String endpoint : OVERPASS_ENDPOINTS) {
            try {
                URI uri = URI.create(endpoint + "?data=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                PlaceResult result = parsePlaces(city, location, JsonUtils.getObjectMapper().readTree(get(uri)));
                if (result != null) return result;
            } catch (Exception ex) {
                log.info("Overpass 节点暂不可用 endpoint={} city={} reason={}", endpoint, city, ex.getMessage());
            }
        }
        return null;
    }

    private PlaceResult parsePlaces(String city, City location, JsonNode root) {
        JsonNode elements = root.path("elements");
        if (!elements.isArray()) return null;
        Map<String, List<PublicTravelMapSnapshot.Place>> candidates = new LinkedHashMap<>();
        candidates.put("attraction", new ArrayList<>());
        candidates.put("hotel", new ArrayList<>());
        candidates.put("restaurant", new ArrayList<>());
        Set<String> seenNames = new HashSet<>();
        String updatedAt = Instant.now().toString();
        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            String name = firstNonBlank(text(tags.path("name:zh")), text(tags.path("name")));
            if (name.isBlank()) continue;
            String tourism = text(tags.path("tourism"));
            String amenity = text(tags.path("amenity"));
            String kind = amenity.matches("restaurant|cafe|fast_food|food_court") ? "餐饮"
                : tourism.matches("hotel|guest_house|hostel|motel") ? "住宿" : "景区";
            MapPoint point = point(element);
            if (point == null) continue;
            String normalizedName = normalizePlaceName(name);
            if (!seenNames.add(normalizedName)) continue;
            String mapKind = "景区".equals(kind) ? "attraction" : "住宿".equals(kind) ? "hotel" : "restaurant";
            List<PublicTravelMapSnapshot.Place> group = candidates.get(mapKind);
            String elementId = text(element.path("id"));
            String elementType = text(element.path("type"));
            group.add(new PublicTravelMapSnapshot.Place(
                elementId.isBlank() ? "osm-" + mapKind + '-' + normalizedName : "osm-" + elementType + '-' + elementId,
                name, mapKind, point.longitude(), point.latitude(),
                address(tags), category(tags), text(tags.path("opening_hours")),
                distanceKm(location, point),
                null, parseDouble(text(tags.path("charge"))), firstNonBlank(text(tags.path("image")),
                    text(tags.path("wikimedia_commons"))), text(tags.path("description")), 0, "",
                "OpenStreetMap", updatedAt));
        }
        Map<String, List<String>> names = new LinkedHashMap<>();
        names.put("景区", new ArrayList<>());
        names.put("住宿", new ArrayList<>());
        names.put("餐饮", new ArrayList<>());
        List<PublicTravelMapSnapshot.Place> places = new ArrayList<>();
        candidates.forEach((kind, group) -> group.stream()
            .sorted(Comparator.comparingDouble(PublicTravelMapSnapshot.Place::distance_km))
            .forEach(place -> {
                places.add(place);
                names.get("attraction".equals(kind) ? "景区" : "hotel".equals(kind) ? "住宿" : "餐饮")
                    .add(place.name());
            }));
        String detail = names.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> entry.getKey() + "：" + String.join("、", entry.getValue()))
            .reduce((left, right) -> left + "；" + right)
            .orElse("");
        if (detail.isBlank()) return null;
        return new PlaceResult(places, new PublicDataItem(
            city + "公开地点资料", detail + "。不含实时价格、库存和营业状态。",
            "OpenStreetMap", updatedAt, "open_data", false,
            "https://www.openstreetmap.org/copyright"
        ));
    }

    private RouteResult loadRoute(PublicTravelMapSnapshot.Place from, PublicTravelMapSnapshot.Place to) {
        try {
            URI uri = URI.create("https://router.project-osrm.org/route/v1/driving/"
                + from.longitude() + "," + from.latitude() + ";"
                + to.longitude() + "," + to.latitude() + "?overview=full&geometries=geojson");
            JsonNode route = JsonUtils.getObjectMapper().readTree(get(uri)).path("routes").path(0);
            if (!route.has("distance") || !route.has("duration")) return null;
            double distance = route.path("distance").asDouble() / 1000;
            long minutes = Math.max(1, Math.round(route.path("duration").asDouble() / 60));
            String detail = "%s → %s · 约 %.1f 公里 · 驾车约 %d 分钟；这是公开地点间的路线示例，不代表本行程安排，也不含实时拥堵。".formatted(
                from.name(), to.name(), distance, minutes);
            String updatedAt = Instant.now().toString();
            List<MapPoint> geometry = new ArrayList<>();
            for (JsonNode coordinate : route.path("geometry").path("coordinates")) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    geometry.add(new MapPoint(coordinate.path(0).asDouble(), coordinate.path(1).asDouble()));
                }
            }
            if (geometry.size() < 2) return null;
            PublicTravelMapSnapshot.Route mapRoute = new PublicTravelMapSnapshot.Route(
                from.name(), to.name(), distance, minutes, geometry, "OSRM / OpenStreetMap", updatedAt,
                "公开地点间的驾车路线估算，不代表本行程顺序，也不含实时拥堵。");
            return new RouteResult(mapRoute, new PublicDataItem("公开景点间路线示例", detail, "OSRM / OpenStreetMap",
                updatedAt, "route_estimate", false, "https://project-osrm.org/"));
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

    private boolean validCoordinates(Double longitude, Double latitude) {
        return longitude != null && latitude != null
            && Double.isFinite(longitude) && Double.isFinite(latitude)
            && longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90;
    }

    private String locationKey(String city, City location) {
        return "%s:%.4f:%.4f".formatted(city, location.longitude(), location.latitude());
    }

    private String address(JsonNode tags) {
        String full = text(tags.path("addr:full"));
        if (!full.isBlank()) return full;
        String street = text(tags.path("addr:street")) + text(tags.path("addr:housenumber"));
        return Stream.of(
                text(tags.path("addr:province")), text(tags.path("addr:state")),
                text(tags.path("addr:city")), text(tags.path("addr:district")),
                text(tags.path("addr:subdistrict")), street)
            .filter(value -> !value.isBlank()).distinct().reduce("", String::concat);
    }

    private String category(JsonNode tags) {
        if (!text(tags.path("historic")).isBlank()) return "历史古迹";
        String tourism = text(tags.path("tourism"));
        if (!tourism.isBlank()) return switch (tourism) {
            case "museum" -> "博物馆";
            case "gallery" -> "美术馆";
            case "viewpoint" -> "观景台";
            case "zoo" -> "动物园";
            case "aquarium" -> "水族馆";
            case "theme_park" -> "主题乐园";
            case "hotel" -> "酒店";
            case "guest_house" -> "民宿";
            case "hostel" -> "青年旅舍";
            case "motel" -> "汽车旅馆";
            default -> "景点";
        };
        String amenity = text(tags.path("amenity"));
        if (!amenity.isBlank()) return switch (amenity) {
            case "cafe" -> "咖啡馆";
            case "fast_food" -> "快餐";
            case "food_court" -> "美食广场";
            default -> "餐厅";
        };
        String leisure = text(tags.path("leisure"));
        if (!leisure.isBlank()) return switch (leisure) {
            case "park" -> "公园";
            case "garden" -> "花园";
            case "nature_reserve" -> "自然保护区";
            default -> "休闲场所";
        };
        return text(tags.path("shop")).isBlank() ? "景点" : "商店";
    }

    private double distanceKm(City location, MapPoint point) {
        double latitudeDistance = Math.toRadians(point.latitude() - location.latitude());
        double longitudeDistance = Math.toRadians(point.longitude() - location.longitude());
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
            + Math.cos(Math.toRadians(location.latitude())) * Math.cos(Math.toRadians(point.latitude()))
            * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return Math.round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10.0;
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

    private String normalizePlaceName(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s·•・—_\\-（）()]", "");
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private record City(
        double longitude, double latitude, String airportCode, String airportName,
        double airportLongitude, double airportLatitude
    ) {
    }

    private record Cached<T>(Instant loadedAt, T value) {
    }

    private record WeatherResult(
        List<MapWeatherForecast> forecasts, double temperature, String condition,
        double windSpeed, String updatedAt, PublicDataItem item
    ) {
    }

    private record PlaceResult(List<PublicTravelMapSnapshot.Place> places, PublicDataItem item) {
    }

    private record RouteResult(PublicTravelMapSnapshot.Route route, PublicDataItem item) {
    }
}
