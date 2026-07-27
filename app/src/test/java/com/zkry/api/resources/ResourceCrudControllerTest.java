package com.zkry.api.resources;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zkry.identity.service.IdentityService;
import com.zkry.resources.service.CrudResourceService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ResourceCrudControllerTest {

    @Test
    void rejectsResourcesThatHaveNoAdminProductSurface() {
        ResourceCrudController controller = new ResourceCrudController(mock(CrudResourceService.class), mock(IdentityService.class));
        assertThatThrownBy(() -> controller.create("favorites", Map.of()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }
}
