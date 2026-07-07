package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.zkry.trip.dto.ai.ContentAnalyzeRequest;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripDayEvaluationInput;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PythonAiClientTest {

    @Test
    void detectVisionParsesPythonEnvelopeData() throws Exception {
        HttpServer server = startJsonServer("/api/vision/detect", """
            {"code":0,"message":"success","data":{"model_mode":"rule","labels":[{"name":"travel_scene","confidence":0.91}],"scene_tags":["travel_scene","food"],"summary":"Hangzhou food image","risk_hints":[],"source":"image_url"}}
            """);
        try {
            PythonAiClient client = new PythonAiClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));

            PythonAiCallResult<VisionDetectResult> result = client.detectVision(
                new VisionDetectRequest("https://example.com/food.jpg", "Hangzhou", "restaurant"));

            assertThat(result.success()).isTrue();
            assertThat(result.data().model_mode()).isEqualTo("rule");
            assertThat(result.data().labels()).hasSize(1);
            assertThat(result.data().scene_tags()).contains("food");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluateTripReturnsFallbackWhenPythonServiceUnavailable() {
        PythonAiClient client = new PythonAiClient("http://127.0.0.1:9", Duration.ofMillis(200));

        PythonAiCallResult<TripEvaluateResult> result = client.evaluateTrip(new TripEvaluateRequest(
            List.of(new TripDayEvaluationInput("2026-08-01", "Hangzhou", List.of("A", "B"), "sunny", false)),
            "公共交通",
            0,
            List.of("轻松"),
            2000D
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Python AI service unavailable");
        assertThat(result.data()).isNull();
    }

    @Test
    void analyzeContentPostsTextPayload() throws Exception {
        HttpServer server = startJsonServer("/api/content/analyze", """
            {"code":0,"message":"success","data":{"sentiment":"mixed","keywords":["西湖"],"positive_highlights":["提到好体验"],"negative_warnings":["注意排队问题"],"suitable_traveler_types":["family"]}}
            """);
        try {
            PythonAiClient client = new PythonAiClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));

            PythonAiCallResult<?> result = client.analyzeContent(
                new ContentAnalyzeRequest("西湖很好但是排队", "杭州", "西湖", "zh"));

            assertThat(result.success()).isTrue();
            assertThat(result.rawJson()).contains("sentiment");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void postsJsonWithoutHttp2UpgradeForFastApiCompatibility() throws Exception {
        AtomicReference<String> upgradeHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/vision/detect", exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            byte[] response = """
                {"code":0,"message":"success","data":{"model_mode":"rule","labels":[],"scene_tags":[],"summary":"ok","risk_hints":[],"source":"image_url"}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            PythonAiClient client = new PythonAiClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));

            PythonAiCallResult<VisionDetectResult> result = client.detectVision(
                new VisionDetectRequest("https://example.com/food.jpg", "Hangzhou", "restaurant"));

            assertThat(result.success()).isTrue();
            assertThat(upgradeHeader.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void omitsNullFieldsWhenPostingTripEvaluationPayload() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/trip/evaluate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"code":0,"message":"success","data":{"comfort_score":88,"risk_level":"low","daily_risks":[],"suggestions":[]}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            PythonAiClient client = new PythonAiClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));

            PythonAiCallResult<TripEvaluateResult> result = client.evaluateTrip(new TripEvaluateRequest(
                List.of(new TripDayEvaluationInput("2026-08-01", "Hangzhou", List.of("西湖"), "晴", null)),
                "公共交通",
                null,
                List.of("轻松"),
                2000D
            ));

            assertThat(result.success()).isTrue();
            assertThat(requestBody.get()).doesNotContain("\"city_transfers\":null");
            assertThat(requestBody.get()).doesNotContain("\"transfer\":null");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startJsonServer(String path, String json) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }
}
