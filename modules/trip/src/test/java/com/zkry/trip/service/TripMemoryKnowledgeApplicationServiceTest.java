package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import com.zkry.resources.service.TripMemoryKnowledgeContract;
import com.zkry.resources.service.TripMemoryService;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TripMemoryKnowledgeApplicationServiceTest {

    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void ownershipPrecedesIndexAndReadyStatusFollowsPythonSuccess() {
        TripMemoryService memory = org.mockito.Mockito.mock(TripMemoryService.class);
        PythonAiClient python = org.mockito.Mockito.mock(PythonAiClient.class);
        var source = new TripMemoryKnowledgeContract.Source(301L, 901L, "杭州旅行", "杭州", List.of());
        var indexed = new TripMemoryKnowledgeContract.IndexResult(301L, 0, "BAAI/bge-small-zh-v1.5");
        when(memory.knowledgeSource(1001L, 301L)).thenReturn(source);
        when(python.indexMemory(org.mockito.ArgumentMatchers.any()))
            .thenReturn(PythonAiCallResult.ok("success", indexed, "{}"));
        var service = new TripMemoryKnowledgeApplicationService(memory, python, SECRET);

        assertThat(service.index(1001L, 301L)).isEqualTo(indexed);

        ArgumentCaptor<TripMemoryKnowledgeContract.IndexRequest> request =
            ArgumentCaptor.forClass(TripMemoryKnowledgeContract.IndexRequest.class);
        verify(python).indexMemory(request.capture());
        assertThat(request.getValue().ownerScope()).hasSize(64).doesNotContain("1001");
        var order = inOrder(memory, python);
        order.verify(memory).knowledgeSource(1001L, 301L);
        order.verify(memory).knowledgeStatus(1001L, 301L, "indexing");
        order.verify(python).indexMemory(org.mockito.ArgumentMatchers.any());
        order.verify(memory).knowledgeStatus(1001L, 301L, "ready");
    }

    @Test
    void askChecksOwnershipAndRunsJavaCitationValidation() {
        TripMemoryService memory = org.mockito.Mockito.mock(TripMemoryService.class);
        PythonAiClient python = org.mockito.Mockito.mock(PythonAiClient.class);
        var raw = new TripMemoryKnowledgeContract.Answer("西湖", List.of(
            new TripMemoryKnowledgeContract.Citation(11L, "trip_item", 81L, "西湖")), true);
        when(memory.knowledgeIdentity(1001L, 301L)).thenReturn(new TripMemoryKnowledgeContract.Identity(301L, 901L));
        when(python.queryMemory(org.mockito.ArgumentMatchers.any())).thenReturn(PythonAiCallResult.ok("success", raw, "{}"));
        when(memory.validateAnswer(1001L, 301L, raw)).thenReturn(raw);
        var service = new TripMemoryKnowledgeApplicationService(memory, python, SECRET);

        assertThat(service.ask(1001L, 301L, " 去了哪里？ ", 99)).isEqualTo(raw);

        ArgumentCaptor<TripMemoryKnowledgeContract.QueryRequest> request =
            ArgumentCaptor.forClass(TripMemoryKnowledgeContract.QueryRequest.class);
        verify(python).queryMemory(request.capture());
        assertThat(request.getValue().question()).isEqualTo("去了哪里？");
        assertThat(request.getValue().topK()).isEqualTo(10);
        verify(memory).validateAnswer(1001L, 301L, raw);
    }

    @Test
    void qdrantFailureProtectsMysqlMemoryDeletion() {
        TripMemoryService memory = org.mockito.Mockito.mock(TripMemoryService.class);
        PythonAiClient python = org.mockito.Mockito.mock(PythonAiClient.class);
        when(memory.knowledgeIdentity(1001L, 301L)).thenReturn(new TripMemoryKnowledgeContract.Identity(301L, 901L));
        when(python.deleteMemory(org.mockito.ArgumentMatchers.any()))
            .thenReturn(PythonAiCallResult.failure("memory vector service unavailable"));
        var service = new TripMemoryKnowledgeApplicationService(memory, python, SECRET);

        assertThatThrownBy(() -> service.delete(1001L, 301L))
            .isInstanceOf(BizException.class).hasMessage("memory vector service unavailable");
        verify(memory, never()).delete(1001L, 301L);
    }
}
