package com.zkry.trip.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zkry.common.json.utils.JsonUtils;
import com.zkry.trip.dto.ai.ContentAnalyzeRequest;
import com.zkry.trip.dto.ai.ContentAnalyzeResult;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import com.zkry.trip.dto.ai.TripEvaluateRequest;
import com.zkry.trip.dto.ai.TripEvaluateResult;
import com.zkry.trip.dto.ai.VisionDetectRequest;
import com.zkry.trip.dto.ai.VisionDetectResult;
import com.zkry.resources.service.TripMemoryAnalysisContract;
import com.zkry.resources.service.TripMemoryKnowledgeContract;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PythonAiClient {

    private final String baseUrl;
    private final Duration timeout;
    private final Duration memoryTimeout;
    private final HttpClient httpClient;
    private final String memoryServiceToken;
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private final ObjectWriter requestWriter = objectMapper.copy()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .writer();

    @Autowired
    public PythonAiClient(
        @Value("${travelmind.python-ai.base-url:http://localhost:19080}") String baseUrl,
        @Value("${travelmind.python-ai.timeout-ms:10000}") long timeoutMs,
        @Value("${travelmind.python-ai.memory-timeout-ms:120000}") long memoryTimeoutMs,
        @Value("${travelmind.python-ai.memory-service-token}") String memoryServiceToken
    ) {
        this(baseUrl, Duration.ofMillis(timeoutMs), Duration.ofMillis(memoryTimeoutMs), memoryServiceToken);
    }

    PythonAiClient(String baseUrl, Duration timeout) {
        this(baseUrl, timeout, timeout, "travelmind-dev-memory-token-change-me");
    }

    PythonAiClient(String baseUrl, Duration timeout, Duration memoryTimeout, String memoryServiceToken) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.timeout = timeout;
        this.memoryTimeout = memoryTimeout;
        this.memoryServiceToken = memoryServiceToken;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build();
    }

    public PythonAiCallResult<VisionDetectResult> detectVision(VisionDetectRequest request) {
        return post("/api/vision/detect", request, VisionDetectResult.class);
    }

    public PythonAiCallResult<TripEvaluateResult> evaluateTrip(TripEvaluateRequest request) {
        return post("/api/trip/evaluate", request, TripEvaluateResult.class);
    }

    public PythonAiCallResult<ContentAnalyzeResult> analyzeContent(ContentAnalyzeRequest request) {
        return post("/api/content/analyze", request, ContentAnalyzeResult.class);
    }

    public PythonAiCallResult<TripMemoryAnalysisContract.Result> analyzeMemory(TripMemoryAnalysisContract.Input request) {
        return postMemory("/api/memory/analyze", request, TripMemoryAnalysisContract.Result.class);
    }

    public PythonAiCallResult<TripMemoryKnowledgeContract.IndexResult> indexMemory(TripMemoryKnowledgeContract.IndexRequest request) {
        return postMemory("/api/memory/index", request, TripMemoryKnowledgeContract.IndexResult.class);
    }

    public PythonAiCallResult<TripMemoryKnowledgeContract.Answer> queryMemory(TripMemoryKnowledgeContract.QueryRequest request) {
        return postMemory("/api/memory/query", request, TripMemoryKnowledgeContract.Answer.class);
    }

    public PythonAiCallResult<TripMemoryKnowledgeContract.DeleteResult> deleteMemory(TripMemoryKnowledgeContract.DeleteRequest request) {
        return postMemory("/api/memory/delete", request, TripMemoryKnowledgeContract.DeleteResult.class);
    }

    private <T> PythonAiCallResult<T> postMemory(String path, Object body, Class<T> responseType) {
        return post(path, body, responseType, memoryServiceToken, memoryTimeout);
    }

    private <T> PythonAiCallResult<T> post(String path, Object body, Class<T> responseType) {
        return post(path, body, responseType, memoryServiceToken, timeout);
    }

    private <T> PythonAiCallResult<T> post(
        String path,
        Object body,
        Class<T> responseType,
        String internalToken,
        Duration requestTimeout
    ) {
        try {
            String json = requestWriter.writeValueAsString(body);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json));
            if (internalToken != null && !internalToken.isBlank()) {
                requestBuilder.header("X-Internal-Service-Token", internalToken);
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return PythonAiCallResult.failure("Python AI service returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            int code = root.path("code").asInt(-1);
            String message = root.path("message").asText("success");
            if (code != 0) {
                return PythonAiCallResult.failure(message);
            }
            T data = objectMapper.treeToValue(root.path("data"), responseType);
            return PythonAiCallResult.ok(message, data, response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return PythonAiCallResult.failure("Python AI service unavailable: request interrupted");
        } catch (IOException | RuntimeException ex) {
            return PythonAiCallResult.failure("Python AI service unavailable: " + ex.getMessage());
        }
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:19080" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
