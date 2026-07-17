package com.zkry.map.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zkry.map.dto.PublicTravelSnapshot;
import java.io.IOException;
import java.net.URI;
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
                    return "{\"routes\":[{\"distance\":8400,\"duration\":1500}]}";
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
              {"lat":30.242,"lon":120.1485,"tags":{"name":"西湖","tourism":"attraction"}},
              {"lat":30.2411,"lon":120.1012,"tags":{"name":"灵隐寺","tourism":"attraction"}},
              {"lat":30.25,"lon":120.15,"tags":{"name":"湖畔酒店","tourism":"hotel"}},
              {"lat":30.26,"lon":120.16,"tags":{"name":"楼外楼","amenity":"restaurant"}}
            ]}
            """;
    }
}
