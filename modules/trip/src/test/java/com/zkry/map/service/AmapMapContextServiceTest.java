package com.zkry.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import com.zkry.common.core.config.TravelMindSettingKeys;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.map.dto.MapPoint;
import com.zkry.map.util.MapCoordinates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AmapMapContextServiceTest {

    @Test
    void searchesAroundWithBoundedPagesDeduplicationAndExtendedFields() throws Exception {
        StubService service = new StubService(settings("test-key"));
        ReflectionTestUtils.setField(service, "enabled", true);

        var pois = service.searchAround(
            new MapPoint(116.403632, 39.910125), "北京", "博物馆", "110000", 99, 10);

        assertThat(service.calls).hasSize(8);
        assertThat(service.calls).extracting(call -> call.get("page_num"))
            .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
        assertThat(service.calls).allSatisfy(call -> {
            assertThat(call).containsEntry("page_size", "25")
                .containsEntry("location", "116.403632,39.910125")
                .containsEntry("keywords", "博物馆")
                .containsEntry("types", "110000")
                .containsEntry("region", "北京")
                .containsEntry("city_limit", "true")
                .containsEntry("show_fields", "business,photos");
        });
        assertThat(service.paths).containsOnly("/v5/place/around");
        assertThat(pois).hasSize(9).extracting("id").containsOnlyOnce("id-1");
        assertThat(pois.get(0).name()).isEqualTo("故宫博物院");
        assertThat(pois.get(0).address()).isEqualTo("景山前街4号");
        assertThat(pois.get(0).type()).isEqualTo("风景名胜;人文景观;博物馆");
        assertThat(pois.get(0).rating()).isEqualTo("4.9");
        assertThat(pois.get(0).distance()).isEqualTo("860");
        assertThat(pois.get(0).photoUrl()).isEqualTo("https://example.com/gugong.jpg");
        assertThat(pois.get(0).openTimeWeek()).isEqualTo("周二至周日09:00-17:00");
        assertThat(pois.get(0).openTimeToday()).isEqualTo("09:00-17:00");
        assertThat(pois.get(0).cost()).isEqualTo("60.00");
        assertThat(pois.get(0).tag()).isEqualTo("世界遗产");
        assertThat(pois.get(0).location().longitude()).isCloseTo(116.397389, within(0.0001));
        assertThat(pois.get(0).location().latitude()).isCloseTo(39.908722, within(0.0001));

        service.calls.clear();
        service.searchAround(new MapPoint(116.403632, 39.910125), "北京", "", "", 0, 0);
        assertThat(service.calls).singleElement().satisfies(call -> assertThat(call)
            .containsEntry("page_size", "1").containsEntry("page_num", "1"));
    }

    @Test
    void convertsMainlandCoordinatesAndLeavesOverseasCoordinatesUntouched() {
        MapPoint beijing = new MapPoint(116.397389, 39.908722);
        MapPoint beijingGcj = MapCoordinates.wgs84ToGcj02(beijing);
        assertThat(beijingGcj.longitude()).isCloseTo(116.403633, within(0.0002));
        assertThat(beijingGcj.latitude()).isCloseTo(39.910125, within(0.0002));
        MapPoint beijingRoundTrip = MapCoordinates.gcj02ToWgs84(beijingGcj);
        assertThat(beijingRoundTrip.longitude()).isCloseTo(beijing.longitude(), within(0.00002));
        assertThat(beijingRoundTrip.latitude()).isCloseTo(beijing.latitude(), within(0.00002));

        MapPoint hangzhou = new MapPoint(120.1551, 30.2741);
        MapPoint hangzhouRoundTrip = MapCoordinates.gcj02ToWgs84(MapCoordinates.wgs84ToGcj02(hangzhou));
        assertThat(hangzhouRoundTrip.longitude()).isCloseTo(hangzhou.longitude(), within(0.00002));
        assertThat(hangzhouRoundTrip.latitude()).isCloseTo(hangzhou.latitude(), within(0.00002));

        MapPoint paris = new MapPoint(2.3522, 48.8566);
        assertThat(MapCoordinates.wgs84ToGcj02(paris)).isSameAs(paris);
        assertThat(MapCoordinates.gcj02ToWgs84(paris)).isSameAs(paris);
    }

    @Test
    void reportsReadinessWithoutCallingTheNetworkOrExposingTheKey() {
        AmapMapContextService disabled = new AmapMapContextService(settings("secret-key"));
        assertThat(disabled.ready()).isFalse();
        ReflectionTestUtils.setField(disabled, "enabled", true);
        assertThat(disabled.ready()).isTrue();

        AmapMapContextService missingKey = new AmapMapContextService(settings(""));
        ReflectionTestUtils.setField(missingKey, "enabled", true);
        assertThat(missingKey.ready()).isFalse();
    }

    private static TravelMindRuntimeSettingsService settings(String key) {
        TravelMindRuntimeSettingsService settings = mock(TravelMindRuntimeSettingsService.class);
        when(settings.stringValue(TravelMindSettingKeys.AMAP_WEB_KEY))
            .thenReturn(key.isBlank() ? Optional.empty() : Optional.of(key));
        return settings;
    }

    private static final class StubService extends AmapMapContextService {

        private final List<String> paths = new ArrayList<>();
        private final List<Map<String, String>> calls = new ArrayList<>();

        private StubService(TravelMindRuntimeSettingsService settings) {
            super(settings);
        }

        @Override
        protected JsonNode get(String path, Map<String, String> params) throws IOException {
            paths.add(path);
            calls.add(new LinkedHashMap<>(params));
            int page = Integer.parseInt(params.get("page_num"));
            String body = page == 1 ? """
                {"pois":[
                  {"id":"id-1","name":"故宫博物院","address":"景山前街4号","location":"116.403632,39.910125",
                   "type":"风景名胜;人文景观;博物馆","distance":"860",
                   "business":{"rating":"4.9","opentime_week":"周二至周日09:00-17:00","opentime_today":"09:00-17:00","cost":"60.00","tag":"世界遗产"},
                   "photos":[{"url":"https://example.com/gugong.jpg"}]},
                  {"id":"","name":"无编号展馆","location":"116.41,39.91","business":{}}
                ]}
                """ : """
                {"pois":[
                  {"id":"id-1","name":"故宫重复项","location":"116.403632,39.910125","business":{}},
                  {"id":"id-%d","name":"分页地点%d","location":"116.42,39.92","business":{}},
                  {"id":"","name":"无 编号展馆","location":"116.43,39.93","business":{}}
                ]}
                """.formatted(page, page);
            return JsonUtils.getObjectMapper().readTree(body);
        }
    }
}
