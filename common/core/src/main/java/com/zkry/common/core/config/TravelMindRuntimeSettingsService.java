package com.zkry.common.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TravelMindRuntimeSettingsService {

    private final Map<String, Object> settings = new LinkedHashMap<>();

    public TravelMindRuntimeSettingsService(
        @Value("${travelmind.map.amap.key:}") String amapWebKey,
        @Value("${travelmind.content.xhs.cookie:}") String xhsCookie,
        @Value("${travelmind.content.xhs.mode:service}") String xhsMode,
        @Value("${spring.ai.dashscope.api-key:}") String dashscopeApiKey,
        @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}") String dashscopeModel
    ) {
        settings.put(TravelMindSettingKeys.AMAP_WEB_KEY, emptyToDefault(amapWebKey));
        settings.put(TravelMindSettingKeys.AMAP_WEB_JS_KEY, "");
        settings.put(TravelMindSettingKeys.GOOGLE_MAPS_API_KEY, "");
        settings.put(TravelMindSettingKeys.GOOGLE_MAPS_PROXY, "");
        settings.put(TravelMindSettingKeys.XHS_COOKIE, emptyToDefault(xhsCookie));
        settings.put(TravelMindSettingKeys.XHS_MODE, emptyToDefault(xhsMode));
        settings.put(TravelMindSettingKeys.OPENAI_API_KEY, emptyToDefault(dashscopeApiKey));
        settings.put(TravelMindSettingKeys.OPENAI_BASE_URL, "");
        settings.put(TravelMindSettingKeys.OPENAI_MODEL, emptyToDefault(dashscopeModel));
    }

    public synchronized Map<String, Object> snapshot() {
        return new LinkedHashMap<>(settings);
    }

    public synchronized void update(Map<String, Object> updates) {
        if (updates == null) {
            return;
        }
        settings.putAll(updates);
    }

    public synchronized Optional<String> stringValue(String key) {
        Object value = settings.get(key);
        if (value == null) {
            return Optional.empty();
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    public synchronized boolean hasText(String key) {
        return stringValue(key).isPresent();
    }

    private String emptyToDefault(String value) {
        return value == null ? "" : value;
    }
}
