package com.zkry.api.resources;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.CommunityService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CommunityControllerTest {

    @Test
    void personalCommunityActionsAlwaysUseCurrentUser() {
        CommunityService service = org.mockito.Mockito.mock(CommunityService.class);
        CommunityController controller = new CommunityController(service);
        Map<String, Object> payload = Map.of("title", "西湖慢游");

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            controller.myPosts(1, 20);
            controller.create(payload);
            controller.bag();
            controller.addToBag(Map.of("post_id", 7001L, "intent", "must"));
            controller.removeFromBag(7001L);
        }

        verify(service).myPosts(1001L, 1, 20);
        verify(service).createPost(1001L, payload);
        verify(service).bag(1001L);
        verify(service).addToBag(1001L, 7001L, "must");
        verify(service).removeFromBag(1001L, 7001L);
    }
}
