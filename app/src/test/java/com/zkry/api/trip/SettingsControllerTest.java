package com.zkry.api.trip;

import static org.assertj.core.api.Assertions.assertThat;

import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SettingsControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void neverReturnsSecretValues() {
        TravelMindRuntimeSettingsService settings = new TravelMindRuntimeSettingsService(
            "amap-secret",
            "cookie-secret",
            "tool",
            "openai-secret",
            "https://api.deepseek.com",
            "deepseek-v4-flash"
        );
        SettingsController controller = new SettingsController(settings, new MockEnvironment());

        Map<String, Object> response = controller.get();
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        assertThat(data.get("vite_amap_web_key")).isEqualTo("configured");
        assertThat(data.get("xhs_cookie")).isEqualTo("configured");
        assertThat(data.get("openai_api_key")).isEqualTo("configured");
        assertThat(data.get("openai_base_url")).isEqualTo("https://api.deepseek.com");
        assertThat(response.toString()).doesNotContain("amap-secret", "cookie-secret", "openai-secret");
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveResponseIsAlsoRedacted() {
        TravelMindRuntimeSettingsService settings = new TravelMindRuntimeSettingsService(
            "", "", "tool", "", "https://api.deepseek.com", "deepseek-v4-flash"
        );
        SettingsController controller = new SettingsController(settings, new MockEnvironment());

        Map<String, Object> response = controller.save(Map.of("openai_api_key", "new-secret"));
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        assertThat(data.get("openai_api_key")).isEqualTo("configured");
        assertThat(response.toString()).doesNotContain("new-secret");
    }
}
