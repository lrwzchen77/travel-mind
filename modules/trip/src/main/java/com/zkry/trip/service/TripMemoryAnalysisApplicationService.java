package com.zkry.trip.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.resources.service.TripMemoryAnalysisContract;
import com.zkry.resources.service.TripMemoryService;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import org.springframework.stereotype.Service;

@Service
public class TripMemoryAnalysisApplicationService {

    private final TripMemoryService memoryService;
    private final PythonAiClient pythonAiClient;

    public TripMemoryAnalysisApplicationService(TripMemoryService memoryService, PythonAiClient pythonAiClient) {
        this.memoryService = memoryService;
        this.pythonAiClient = pythonAiClient;
    }

    public TripMemoryAnalysisContract.Saved analyze(long userId, long memoryId) {
        TripMemoryAnalysisContract.Input input = memoryService.analysisInput(userId, memoryId);
        memoryService.analysisStatus(userId, memoryId, "processing");
        try {
            PythonAiCallResult<TripMemoryAnalysisContract.Result> call = pythonAiClient.analyzeMemory(input);
            if (!call.success() || call.data() == null) throw new BizException(call.message());
            return memoryService.saveAnalysis(userId, memoryId, call.data());
        } catch (RuntimeException ex) {
            memoryService.analysisStatus(userId, memoryId, "failed");
            throw ex;
        }
    }
}
