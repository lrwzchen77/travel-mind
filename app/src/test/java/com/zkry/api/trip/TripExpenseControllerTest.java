package com.zkry.api.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.TripExpenseService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TripExpenseControllerTest {

    @Test
    void alwaysDelegatesWithCurrentUserAndTrip() {
        TripExpenseService service = org.mockito.Mockito.mock(TripExpenseService.class);
        TripExpenseController controller = new TripExpenseController(service);
        Map<String, Object> summary = Map.of("budget", new BigDecimal("1000"), "actual", BigDecimal.ZERO, "remaining", new BigDecimal("1000"), "items", List.of());
        Map<String, Object> payload = Map.of("category", "food", "title", "午餐", "amount", "88.50");
        when(service.summary(1001L, 9001L)).thenReturn(summary);
        when(service.create(1001L, 9001L, payload)).thenReturn(summary);

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            R<Map<String, Object>> read = controller.summary(9001L);
            R<Map<String, Object>> created = controller.create(9001L, payload);
            controller.delete(9001L, 7001L);

            assertThat(read.getData()).isEqualTo(summary);
            assertThat(created.getData()).isEqualTo(summary);
        }

        verify(service).summary(1001L, 9001L);
        verify(service).create(1001L, 9001L, payload);
        verify(service).delete(1001L, 9001L, 7001L);
    }
}
