package com.zkry.common.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TravelMindRuntimeSettingsService {

    private static final String CONFIGURED_VALUE = "configured";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        TravelMindSettingKeys.AMAP_WEB_KEY,
        TravelMindSettingKeys.AMAP_WEB_JS_KEY,
        TravelMindSettingKeys.GOOGLE_MAPS_API_KEY,
        TravelMindSettingKeys.XHS_COOKIE,
        TravelMindSettingKeys.OPENAI_API_KEY
    );

    private final Map<String, Object> settings = new LinkedHashMap<>();

    public TravelMindRuntimeSettingsService(
        @Value("${travelmind.map.amap.key:}") String amapWebKey,
        @Value("${travelmind.content.xhs.cookie:}") String xhsCookie,
        @Value("${travelmind.content.xhs.mode:service}") String xhsMode,
        @Value("${spring.ai.openai.api-key:}") String openaiApiKey,
        @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String openaiBaseUrl,
        @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String openaiModel
    ) {
        settings.put(TravelMindSettingKeys.AMAP_WEB_KEY, emptyToDefault(amapWebKey));
        settings.put(TravelMindSettingKeys.AMAP_WEB_JS_KEY, "");
        settings.put(TravelMindSettingKeys.GOOGLE_MAPS_API_KEY, "");
        settings.put(TravelMindSettingKeys.GOOGLE_MAPS_PROXY, "");
        settings.put(TravelMindSettingKeys.XHS_COOKIE, emptyToDefault(xhsCookie));
        settings.put(TravelMindSettingKeys.XHS_MODE, emptyToDefault(xhsMode));
        settings.put(TravelMindSettingKeys.OPENAI_API_KEY, emptyToDefault(openaiApiKey));
        settings.put(TravelMindSettingKeys.OPENAI_BASE_URL, emptyToDefault(openaiBaseUrl));
        settings.put(TravelMindSettingKeys.OPENAI_MODEL, emptyToDefault(openaiModel));
    }

    public synchronized Map<String, Object> snapshot() {
        return new LinkedHashMap<>(settings);
    }

    public synchronized Map<String, Object> publicSnapshot() {
        Map<String, Object> result = new LinkedHashMap<>(settings);
        SENSITIVE_KEYS.forEach(key -> {
            Object value = result.get(key);
            result.put(key, hasValue(value) ? CONFIGURED_VALUE : "");
        });
        return result;
    }

    public synchronized void update(Map<String, Object> updates) {
        if (updates == null) {
            return;
        }
        updates.forEach((key, value) -> {
            if (!settings.containsKey(key)) {
                throw new IllegalArgumentException("Unknown runtime setting: " + key);
            }
            String text = value == null ? "" : String.valueOf(value).trim();
            if (text.length() > 8192) {
                throw new IllegalArgumentException("Runtime setting is too long: " + key);
            }
            if (TravelMindSettingKeys.XHS_MODE.equals(key) && !Set.of("service", "tool", "both").contains(text)) {
                throw new IllegalArgumentException("Invalid Xiaohongshu mode");
            }
            settings.put(key, text);
        });
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

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }
}
