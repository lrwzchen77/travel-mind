package com.zkry.trip.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.resources.service.TripMemoryKnowledgeContract;
import com.zkry.resources.service.TripMemoryService;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** JWT ownership remains in Java; Python only receives an opaque, HMAC-scoped owner key. */
@Service
public class TripMemoryKnowledgeApplicationService {

    private final TripMemoryService memoryService;
    private final PythonAiClient pythonAiClient;
    private final byte[] scopeSecret;

    @Autowired
    public TripMemoryKnowledgeApplicationService(
        TripMemoryService memoryService,
        PythonAiClient pythonAiClient,
        @Value("${travelmind.python-ai.memory-scope-secret}") String scopeSecret
    ) {
        this(memoryService, pythonAiClient, scopeSecret.getBytes(StandardCharsets.UTF_8));
    }

    TripMemoryKnowledgeApplicationService(TripMemoryService memoryService, PythonAiClient pythonAiClient, byte[] scopeSecret) {
        this.memoryService = memoryService;
        this.pythonAiClient = pythonAiClient;
        this.scopeSecret = scopeSecret.clone();
        if (this.scopeSecret.length < 32) throw new IllegalArgumentException("memory scope secret must contain at least 32 bytes");
    }

    public TripMemoryKnowledgeContract.IndexResult index(long userId, long memoryId) {
        TripMemoryKnowledgeContract.Source source = memoryService.knowledgeSource(userId, memoryId);
        String scope = ownerScope(userId);
        memoryService.knowledgeStatus(userId, memoryId, "indexing");
        try {
            var request = new TripMemoryKnowledgeContract.IndexRequest(source.memoryId(), source.tripId(), scope,
                source.title(), source.destinationCity(), source.items());
            PythonAiCallResult<TripMemoryKnowledgeContract.IndexResult> call = pythonAiClient.indexMemory(request);
            if (!call.success() || call.data() == null) throw new BizException(call.message());
            memoryService.knowledgeStatus(userId, memoryId, "ready");
            return call.data();
        } catch (RuntimeException ex) {
            memoryService.knowledgeStatus(userId, memoryId, "unavailable");
            throw ex;
        }
    }

    public TripMemoryKnowledgeContract.Answer ask(long userId, long memoryId, String question, int topK) {
        memoryService.knowledgeIdentity(userId, memoryId);
        String cleanQuestion = question == null ? "" : question.trim();
        if (cleanQuestion.isBlank() || cleanQuestion.length() > 500) throw new BizException("问题不能为空且最多 500 字。");
        int safeTopK = Math.min(Math.max(topK, 1), 10);
        var request = new TripMemoryKnowledgeContract.QueryRequest(memoryId, ownerScope(userId), cleanQuestion, safeTopK);
        PythonAiCallResult<TripMemoryKnowledgeContract.Answer> call = pythonAiClient.queryMemory(request);
        if (!call.success() || call.data() == null) throw new BizException(call.message());
        return memoryService.validateAnswer(userId, memoryId, call.data());
    }

    public void delete(long userId, long memoryId) {
        memoryService.knowledgeIdentity(userId, memoryId);
        PythonAiCallResult<TripMemoryKnowledgeContract.DeleteResult> call = pythonAiClient.deleteMemory(
            new TripMemoryKnowledgeContract.DeleteRequest(memoryId, ownerScope(userId)));
        if (!call.success() || call.data() == null || !call.data().deleted()) {
            throw new BizException(call.message());
        }
        memoryService.delete(userId, memoryId);
    }

    private String ownerScope(long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(scopeSecret, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(Long.toString(userId).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot create memory owner scope", ex);
        }
    }
}
