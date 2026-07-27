package com.zkry.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import com.zkry.common.satoken.core.LoginHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadControllerTest {

    @TempDir
    Path temp;

    @Test
    void storesValidImageUnderAuthenticatedOwnerAndPreventsCrossUserRead() throws Exception {
        ImageUploadController controller = new ImageUploadController(temp);
        byte[] png = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0
        };

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1001L);
            String url = controller.image(new MockMultipartFile("image", "photo.png", "image/png", png))
                .getData().get("url");
            String name = url.substring(url.lastIndexOf('/') + 1);

            assertThat(url).startsWith("/private-uploads/1001/");
            assertThat(Files.readAllBytes(temp.resolve("private/1001").resolve(name))).isEqualTo(png);
            assertThat(controller.image(name).getStatusCode()).isEqualTo(HttpStatus.OK);

            login.when(LoginHelper::getUserId).thenReturn(1002L);
            assertThat(controller.image(name).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void rejectsContentTypeAndFileSignatureMismatch() {
        ImageUploadController controller = new ImageUploadController(temp);
        MockMultipartFile disguised = new MockMultipartFile(
            "image", "fake.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> controller.image(disguised))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JPG、PNG 或 WebP");
    }
}
