package com.zkry.api.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.CrudResourceService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class UserLibraryControllerTest {

    @Test
    void userNoteCannotBypassCommunityModerationFields() {
        CrudResourceService service = org.mockito.Mockito.mock(CrudResourceService.class);
        UserLibraryController controller = new UserLibraryController(service);
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            controller.create("travel-notes", Map.of(
                "title", "私密笔记", "content", "内容", "visibility", "public", "status", 1, "user_id", 9001L));
        }
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(service).create(org.mockito.ArgumentMatchers.eq("travel-notes"), payload.capture());
        assertThat(payload.getValue()).containsEntry("user_id", 1001L)
            .containsEntry("visibility", "private").containsEntry("status", 1)
            .doesNotContainValue(9001L);
    }

    @Test
    void editingPublicNoteReturnsItToPendingReview() {
        CrudResourceService service = org.mockito.Mockito.mock(CrudResourceService.class);
        when(service.detail("travel-notes", 7L)).thenReturn(Map.of(
            "id", 7L, "user_id", 1001L, "visibility", "public", "status", 1));
        UserLibraryController controller = new UserLibraryController(service);
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            controller.update("travel-notes", 7L, Map.of("title", "修改后", "content", "新内容", "status", 1));
        }
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(service).update(org.mockito.ArgumentMatchers.eq("travel-notes"), org.mockito.ArgumentMatchers.eq(7L), payload.capture());
        assertThat(payload.getValue()).containsEntry("status", 0).containsKey("review_reason");
        assertThat(payload.getValue().get("review_reason")).isNull();
    }
}
