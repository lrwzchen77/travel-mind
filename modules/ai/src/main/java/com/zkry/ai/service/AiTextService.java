package com.zkry.ai.service;

import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import com.zkry.common.core.config.TravelMindSettingKeys;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

/**
 * 文本 LLM 调用。当前默认对接 DeepSeek（OpenAI 兼容接口）。
 */
@Service
public class AiTextService {

    private static final Logger log = LoggerFactory.getLogger(AiTextService.class);
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private final TravelMindRuntimeSettingsService runtimeSettingsService;

    public AiTextService(TravelMindRuntimeSettingsService runtimeSettingsService) {
        this.runtimeSettingsService = runtimeSettingsService;
    }

    public boolean isAvailable() {
        boolean available = runtimeSettingsService.hasText(TravelMindSettingKeys.OPENAI_API_KEY);
        log.debug("[AI] 运行时 AI 配置可用性检查 available={}", available);
        return available;
    }

    public String modelName() {
        return runtimeSettingsService.stringValue(TravelMindSettingKeys.OPENAI_MODEL).orElse(DEFAULT_MODEL);
    }

    /**
     * 统一封装文本生成。业务层只关心是否生成出文本；失败返回 empty。
     */
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        Optional<ChatModel> chatModel = chatModel();
        if (chatModel.isEmpty()) {
            log.info("[AI] API Key 未配置，跳过 LLM 调用 systemPromptLength={} userPromptLength={}",
                length(systemPrompt), length(userPrompt));
            return Optional.empty();
        }
        long startedAt = System.currentTimeMillis();
        log.info("[AI] 开始调用 LLM systemPromptLength={} userPromptLength={} modelClass={}",
            length(systemPrompt), length(userPrompt), chatModel.get().getClass().getSimpleName());
        try {
            String content = ChatClient.create(chatModel.get())
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            if (content == null || content.isBlank()) {
                log.warn("[AI] LLM 返回空内容 elapsedMs={}", System.currentTimeMillis() - startedAt);
                return Optional.empty();
            }
            log.info("[AI] LLM 调用成功 responseLength={} elapsedMs={}",
                content.length(), System.currentTimeMillis() - startedAt);
            return Optional.of(content.trim());
        } catch (Exception ex) {
            log.warn("[AI] LLM 调用失败 elapsedMs={} reason={}",
                System.currentTimeMillis() - startedAt, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ChatModel> chatModel() {
        Optional<String> apiKey = runtimeSettingsService.stringValue(TravelMindSettingKeys.OPENAI_API_KEY);
        if (apiKey.isEmpty()) {
            return Optional.empty();
        }
        String model = runtimeSettingsService.stringValue(TravelMindSettingKeys.OPENAI_MODEL).orElse(DEFAULT_MODEL);
        String baseUrl = runtimeSettingsService.stringValue(TravelMindSettingKeys.OPENAI_BASE_URL)
            .orElse(DEFAULT_BASE_URL);
        try {
            OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey.get())
                .baseUrl(normalizeBaseUrl(baseUrl))
                .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();
            return Optional.of(OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build());
        } catch (Exception ex) {
            log.warn("[AI] OpenAI 兼容 ChatModel 创建失败 reason={}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = baseUrl.trim();
        // 去掉末尾斜杠，避免 //v1
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
