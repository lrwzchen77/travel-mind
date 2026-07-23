package com.zkry.map.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zkry.map.dto.PublicTravelSnapshot;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicTravelDataServiceTest {

    @Test
    void collectsPublicDataWithOverpassFallbackAndCachesSuccessfulResults() {
        List<String> calls = new ArrayList<>();
        PublicTravelDataService service = new PublicTravelDataService() {
            @Override
            protected String get(URI uri) throws IOException {
                calls.add(uri.getHost());
                if ("api.open-meteo.com".equals(uri.getHost())) return weatherJson();
                if ("overpass.kumi.systems".equals(uri.getHost())) throw new IOException("busy");
                if ("overpass-api.de".equals(uri.getHost())) return placesJson();
                if ("router.project-osrm.org".equals(uri.getHost())) {
                    return "{\"routes\":[{\"distance\":8400,\"duration\":1500,\"geometry\":{\"coordinates\":[[120.1485,30.242],[120.1012,30.2411]]}}]}";
                }
                throw new IOException("unexpected host");
            }
        };

        PublicTravelSnapshot first = service.collect("杭州市");
        PublicTravelSnapshot second = service.collect("杭州");

        assertThat(first.weather()).hasSize(2);
        assertThat(first.items()).extracting("data_kind")
            .containsExactly("live", "open_data", "route_estimate", "open_data");
        assertThat(first.items().get(1).detail()).contains("西湖", "湖畔酒店", "楼外楼", "不含实时价格");
        assertThat(first.items().get(2).detail()).contains("8.4 公里", "25 分钟", "不代表本行程安排", "不含实时拥堵");
        assertThat(first.items().get(3).title()).contains("HGH");
        assertThat(second.weather()).isEqualTo(first.weather());
        assertThat(second.items().subList(0, 3)).isEqualTo(first.items().subList(0, 3));
        var map = service.collectMap("杭州");
        assertThat(map.places()).hasSize(4).extracting("kind")
            .containsExactly("attraction", "attraction", "hotel", "restaurant");
        assertThat(map.places().get(0).address()).isEqualTo("杭州市西湖区龙井路1号");
        assertThat(map.places().get(0).opening_hours()).isEqualTo("Mo-Su 00:00-24:00");
        assertThat(map.places().get(0).distance_km()).isPositive();
        assertThat(map.route().geometry()).hasSize(2);
        assertThat(map.airport().code()).isEqualTo("HGH");
        assertThat(map.availability().weather()).isEqualTo("available");
        assertThat(calls).containsExactly(
            "api.open-meteo.com", "overpass.kumi.systems", "overpass-api.de", "router.project-osrm.org"
        );
    }

    @Test
    void omitsLiveWeatherWhenThePublicServiceFails() {
        PublicTravelDataService service = new PublicTravelDataService() {
            @Override
            protected String get(URI uri) throws IOException {
                if ("api.open-meteo.com".equals(uri.getHost())) throw new IOException("offline");
                return "{\"elements\":[]}";
            }
        };

        PublicTravelSnapshot result = service.collect("北京");

        assertThat(result.weather()).isEmpty();
        assertThat(result.items()).extracting("data_kind").containsOnly("open_data");
        assertThat(service.collect("上海")).isEqualTo(PublicTravelSnapshot.empty());
    }

    @Test
    void loadsMapDataForASelectedCityUsingValidatedCoordinates() {
        PublicTravelDataService service = new PublicTravelDataService() {
            @Override
            protected String get(URI uri) throws IOException {
                if ("api.open-meteo.com".equals(uri.getHost())) return weatherJson();
                if ("router.project-osrm.org".equals(uri.getHost())) {
                    return "{\"routes\":[{\"distance\":8400,\"duration\":1500,\"geometry\":{\"coordinates\":[[118.08,24.47],[118.13,24.44]]}}]}";
                }
                return placesJson();
            }
        };

        var map = service.collectMap("厦门", 118.0894, 24.4798);

        assertThat(map.city()).isEqualTo("厦门");
        assertThat(map.weather()).isNotNull();
        assertThat(map.places()).isNotEmpty();
        assertThat(map.airport()).isNull();
        assertThat(service.collectMap("厦门", 999.0, 24.4798).weather()).isNull();
    }

    @Test
    void expandsPlaceTypesWithoutDroppingCandidates() {
        List<String> queries = new ArrayList<>();
        PublicTravelDataService service = new PublicTravelDataService() {
            @Override
            protected String get(URI uri) throws IOException {
                if ("api.open-meteo.com".equals(uri.getHost())) return weatherJson();
                if ("router.project-osrm.org".equals(uri.getHost())) {
                    return "{\"routes\":[{\"distance\":1000,\"duration\":600,\"geometry\":{\"coordinates\":[[120.15,30.28],[120.16,30.29]]}}]}";
                }
                queries.add(URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8));
                return expandedPlacesJson();
            }
        };

        var map = service.collectMap("杭州");

        assertThat(map.places()).hasSize(64);
        assertThat(map.places()).filteredOn(place -> "attraction".equals(place.kind())).hasSize(22);
        assertThat(map.places()).filteredOn(place -> "hotel".equals(place.kind())).hasSize(21);
        assertThat(map.places()).filteredOn(place -> "restaurant".equals(place.kind())).hasSize(21);
        assertThat(map.places()).extracting("category")
            .contains("博物馆", "美术馆", "观景台", "历史古迹", "公园", "民宿", "青年旅舍", "咖啡馆", "美食广场");
        assertThat(map.places()).extracting("name").contains("城市博物馆").doesNotContain("City Museum");
        assertThat(map.places().get(0).address()).isEqualTo("测试路1号");
        assertThat(map.places().get(0).opening_hours()).isEqualTo("Tu-Su 09:00-17:00");
        assertThat(map.places().get(0).distance_km()).isBetween(0.1, 0.2);
        assertThat(map.places().get(1).address()).isEqualTo("浙江省杭州市西湖区曙光路2号");
        assertThat(queries).singleElement().asString()
            .contains("gallery|viewpoint|zoo|aquarium|theme_park", "[historic]", "park|garden|nature_reserve",
                "hotel|guest_house|hostel|motel", "restaurant|cafe|fast_food|food_court", "out center tags 120");
    }

    @Test
    void keepsEveryAttractionAndSortsNearbyCandidatesFirst() {
        PublicTravelDataService service = new PublicTravelDataService() {
            @Override
            protected String get(URI uri) throws IOException {
                if ("api.open-meteo.com".equals(uri.getHost())) return weatherJson();
                if ("router.project-osrm.org".equals(uri.getHost())) {
                    return "{\"routes\":[{\"distance\":1000,\"duration\":600,\"geometry\":{\"coordinates\":[[120.15,30.28],[120.16,30.29]]}}]}";
                }
                return distanceRankedPlacesJson();
            }
        };

        var attractions = service.collectMap("杭州").places().stream()
            .filter(place -> "attraction".equals(place.kind())).toList();

        assertThat(attractions).hasSize(23).extracting("name")
            .contains("近处古迹", "近处公园", "远景点18", "远景点19", "远景点20");
        assertThat(attractions).extracting("category").contains("历史古迹", "公园");
    }

    private static String weatherJson() {
        return """
            {
              "current":{"time":"2026-07-17T13:00","temperature_2m":36.6,"weather_code":61,"wind_speed_10m":10.8},
              "daily":{"time":["2026-08-01","2026-08-02"],"weather_code":[1,61],
                "temperature_2m_max":[31.4,29.6],"temperature_2m_min":[24.1,23.2]}
            }
            """;
    }

    private static String placesJson() {
        return """
            {"elements":[
              {"lat":30.242,"lon":120.1485,"tags":{"name":"西湖","tourism":"attraction","addr:full":"杭州市西湖区龙井路1号","opening_hours":"Mo-Su 00:00-24:00"}},
              {"lat":30.2411,"lon":120.1012,"tags":{"name":"灵隐寺","tourism":"attraction"}},
              {"lat":30.25,"lon":120.15,"tags":{"name":"湖畔酒店","tourism":"hotel"}},
              {"lat":30.26,"lon":120.16,"tags":{"name":"楼外楼","amenity":"restaurant"}}
            ]}
            """;
    }

    private static String expandedPlacesJson() {
        StringBuilder json = new StringBuilder("{\"elements\":[")
            .append("{\"lat\":30.2751,\"lon\":120.1551,\"tags\":{\"name\":\"City Museum\",\"name:zh\":\"城市博物馆\",\"tourism\":\"museum\",\"addr:full\":\"测试路1号\",\"opening_hours\":\"Tu-Su 09:00-17:00\"}},")
            .append("{\"lat\":30.2751,\"lon\":120.1551,\"tags\":{\"name\":\"城市博物馆\",\"tourism\":\"museum\"}},");
        String[] attractions = {"gallery", "viewpoint", "zoo", "aquarium", "theme_park", "historic", "park", "garden"};
        for (int i = 0; i < 21; i++) {
            String type = attractions[i % attractions.length];
            String tag = "historic".equals(type) ? "\"historic\":\"monument\""
                : ("park".equals(type) || "garden".equals(type)) ? "\"leisure\":\"" + type + "\""
                : "\"tourism\":\"" + type + "\"";
            String address = i == 0 ? ",\"addr:province\":\"浙江省\",\"addr:city\":\"杭州市\",\"addr:district\":\"西湖区\",\"addr:street\":\"曙光路\",\"addr:housenumber\":\"2号\"" : "";
            json.append("{\"lat\":30.28,\"lon\":120.16,\"tags\":{\"name\":\"景点")
                .append(i).append("\",").append(tag).append(address).append("}},");
        }
        String[] hotels = {"hotel", "guest_house", "hostel", "motel"};
        for (int i = 0; i < 21; i++) {
            json.append("{\"lat\":30.29,\"lon\":120.17,\"tags\":{\"name\":\"住宿")
                .append(i).append("\",\"tourism\":\"").append(hotels[i % hotels.length]).append("\"}},");
        }
        String[] restaurants = {"restaurant", "cafe", "fast_food", "food_court"};
        for (int i = 0; i < 21; i++) {
            json.append("{\"lat\":30.30,\"lon\":120.18,\"tags\":{\"name\":\"餐饮")
                .append(i).append("\",\"amenity\":\"").append(restaurants[i % restaurants.length]).append("\"}}")
                .append(i == 20 ? "" : ",");
        }
        return json.append("]}").toString();
    }

    private static String distanceRankedPlacesJson() {
        StringBuilder json = new StringBuilder("{\"elements\":[");
        for (int i = 0; i < 21; i++) {
            json.append("{\"lat\":30.35,\"lon\":120.2,\"tags\":{\"name\":\"远景点")
                .append(i).append("\",\"tourism\":\"attraction\"}},");
        }
        return json
            .append("{\"lat\":30.275,\"lon\":120.155,\"tags\":{\"name\":\"近处古迹\",\"tourism\":\"attraction\",\"historic\":\"monument\"}},")
            .append("{\"lat\":30.276,\"lon\":120.155,\"tags\":{\"name\":\"近处公园\",\"leisure\":\"park\"}}]}")
            .toString();
    }
}
