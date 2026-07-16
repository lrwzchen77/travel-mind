package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import com.zkry.resources.service.TripMemoryAnalysisContract;
import com.zkry.resources.service.TripMemoryService;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class TripMemoryAnalysisApplicationServiceTest {

    @Test
    void checksOwnershipCallsPythonThenPersistsWithSameUserScope() {
        TripMemoryService memory = org.mockito.Mockito.mock(TripMemoryService.class);
        PythonAiClient python = org.mockito.Mockito.mock(PythonAiClient.class);
        var input = new TripMemoryAnalysisContract.Input(301L, 901L, "杭州旅行", "杭州", List.of());
        var result = new TripMemoryAnalysisContract.Result(List.of(),
            new TripMemoryAnalysisContract.Generation("timeline", "第一天", List.of()));
        when(memory.analysisInput(1001L, 301L)).thenReturn(input);
        when(python.analyzeMemory(input)).thenReturn(PythonAiCallResult.ok("success", result, "{}"));
        when(memory.saveAnalysis(1001L, 301L, result)).thenReturn(new TripMemoryAnalysisContract.Saved(501L, 1));
        TripMemoryAnalysisApplicationService service = new TripMemoryAnalysisApplicationService(memory, python);

        TripMemoryAnalysisContract.Saved saved = service.analyze(1001L, 301L);

        assertThat(saved.generationId()).isEqualTo(501L);
        InOrder order = inOrder(memory, python);
        order.verify(memory).analysisInput(1001L, 301L);
        order.verify(memory).analysisStatus(1001L, 301L, "processing");
        order.verify(python).analyzeMemory(input);
        order.verify(memory).saveAnalysis(1001L, 301L, result);
    }

    @Test
    void failureOnlyMarksStatusAndKeepsExistingAnalysisData() {
        TripMemoryService memory = org.mockito.Mockito.mock(TripMemoryService.class);
        PythonAiClient python = org.mockito.Mockito.mock(PythonAiClient.class);
        var input = new TripMemoryAnalysisContract.Input(301L, 901L, "杭州旅行", "杭州", List.of());
        when(memory.analysisInput(1001L, 301L)).thenReturn(input);
        when(python.analyzeMemory(input)).thenReturn(PythonAiCallResult.failure("Python unavailable"));
        TripMemoryAnalysisApplicationService service = new TripMemoryAnalysisApplicationService(memory, python);

        assertThatThrownBy(() -> service.analyze(1001L, 301L))
            .isInstanceOf(BizException.class).hasMessage("Python unavailable");
        verify(memory).analysisStatus(1001L, 301L, "failed");
        verify(memory, never()).saveAnalysis(org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
