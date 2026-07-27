package com.zkry.api.trip;

import java.util.Map;
import java.util.LinkedHashMap;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.zkry.common.redis.util.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final RedisUtils redis;
    private final URI pythonHealth;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public HealthController(
        JdbcTemplate jdbc,
        RedisUtils redis,
        @Value("${travelmind.python-ai.base-url}") String pythonBaseUrl
    ) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.pythonHealth = URI.create(pythonBaseUrl.replaceAll("/+$", "") + "/ready");
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "healthy",
            "service", "travel-mind",
            "mode", "liveness"
        );
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("mysql", mysqlReady() ? "ready" : "unavailable");
        components.put("redis", redisReady() ? "ready" : "degraded");
        components.put("python_ai", pythonReady() ? "ready" : "degraded");
        boolean ready = "ready".equals(components.get("mysql"));
        String status = !ready ? "unavailable" : components.values().stream().allMatch("ready"::equals) ? "ready" : "degraded";
        Map<String, Object> body = Map.of("status", status, "service", "travel-mind", "components", components);
        return ready ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }

    private boolean mysqlReady() {
        try {
            return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean redisReady() {
        try (var connection = redis.getStringRedisTemplate().getConnectionFactory().getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean pythonReady() {
        try {
            HttpRequest request = HttpRequest.newBuilder(pythonHealth).timeout(Duration.ofSeconds(3)).GET().build();
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }
}
