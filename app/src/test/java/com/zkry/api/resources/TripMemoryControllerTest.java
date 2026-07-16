package com.zkry.api.resources;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.TripMemoryService;
import com.zkry.trip.service.TripMemoryAnalysisApplicationService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TripMemoryControllerTest {

    @Test
    void everyMemoryActionUsesJwtUserInsteadOfPayloadIdentity() {
        TripMemoryService service = org.mockito.Mockito.mock(TripMemoryService.class);
        TripMemoryAnalysisApplicationService analysisService = org.mockito.Mockito.mock(TripMemoryAnalysisApplicationService.class);
        TripMemoryController controller = new TripMemoryController(service, analysisService);
        Map<String, Object> photo = Map.of("url", "/uploads/123e4567-e89b-12d3-a456-426614174000.jpg");

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            controller.create(9001L);
            controller.list(1, 20);
            controller.detail(3001L);
            controller.addPhoto(3001L, photo);
            controller.analyze(3001L);
            controller.deleteItem(3001L, 4001L);
            controller.delete(3001L);
        }

        verify(service).createFromTrip(1001L, 9001L);
        verify(service).list(1001L, 1, 20);
        verify(service).detail(1001L, 3001L);
        verify(service).addPhoto(1001L, 3001L, photo);
        verify(analysisService).analyze(1001L, 3001L);
        verify(service).deleteItem(1001L, 3001L, 4001L);
        verify(service).delete(1001L, 3001L);
    }
}
