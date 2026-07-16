package com.zkry.ai.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.zkry.ai.agent.TravelMindAgent;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final AiTextService aiTextService;

    public AiAgentService(AiTextService aiTextService) {
        this.aiTextService = aiTextService;
    }

    public boolean isAvailable() {
        return aiTextService.isAvailable();
    }

    /** 无工具对话的增量输出；流失败时返回空流，由调用方决定降级内容。 */
    public Flux<String> stream(TravelMindAgent agent, String instruction, String userPrompt) {
        Optional<ChatModel> chatModel = aiTextService.chatModel();
        if (chatModel.isEmpty()) {
            return Flux.empty();
        }
        return Flux.defer(() -> ChatClient.create(chatModel.get())
                .prompt()
                .system(instruction)
                .user(userPrompt)
                .stream()
                .content())
            .filter(part -> part != null && !part.isEmpty())
            .onErrorResume(ex -> {
                log.warn("[AI-AGENT] 流式调用失败 reason={}", ex.getMessage());
                return Flux.empty();
            });
    }

    /**
     * 调用一个不带工具的 ReactAgent。
     *
     * <p>适合 Planner/Review 这类只根据上下文生成结构化结果的 Agent。
     */
    public Optional<String> call(TravelMindAgent agent, String instruction, String userPrompt, String threadId) {
        return call(agent, instruction, userPrompt, threadId, new Object[0]);
    }

    /**
     * 调用一个可带 methodTools 的 ReactAgent。
     *
     * <p>工具对象来自 Spring Bean，例如 {@code AmapTravelTools}、
     * {@code XhsTravelTools}。ReactAgent 会根据提示词和用户需求自行决定调用哪个
     * {@code @Tool} 方法。
     */
    public Optional<String> call(
        TravelMindAgent agent,
        String instruction,
        String userPrompt,
        String threadId,
        Object... methodTools
    ) {
        return call(agent.id(), instruction, userPrompt, threadId, methodTools);
    }

    /**
     * 兼容字符串 Agent 名称的入口。业务代码优先使用 {@link TravelMindAgent}。
     */
    public Optional<String> call(String agentName, String instruction, String userPrompt, String threadId) {
        return call(agentName, instruction, userPrompt, threadId, new Object[0]);
    }

    /**
     * ReactAgent 底层调用入口。
     *
     * <p>这个方法只负责“把模型、系统指令、用户提示词和工具拼成 Agent 并调用”。
     * 返回值仍是原始文本；如果需要 DTO，请走 {@link AiStructuredOutputService}。
     */
    public Optional<String> call(
        String agentName,
        String instruction,
        String userPrompt,
        String threadId,
        Object... methodTools
    ) {
        Optional<ChatModel> chatModel = aiTextService.chatModel();
        if (chatModel.isEmpty()) {
            log.info("[AI-AGENT] ChatModel 不可用，跳过 Agent 调用 agent={}", agentName);
            return Optional.empty();
        }
        long startedAt = System.currentTimeMillis();
        try {
            ReactAgent agent = ReactAgent.builder()
                .name(agentName)
                .model(chatModel.get())
                .instruction(instruction)
                .enableLogging(true)
                .methodTools(methodTools == null ? new Object[0] : methodTools)
                .build();
            String safeThreadId = threadId == null || threadId.isBlank() ? agentName : threadId;
            RunnableConfig config = RunnableConfig.builder()
                .threadId(safeThreadId)
                .build();
            log.info("[AI-AGENT] 开始调用 ReactAgent agent={} threadId={} instructionLength={} promptLength={} toolCount={} tools={}",
                agentName,
                safeThreadId,
                length(instruction),
                length(userPrompt),
                methodTools == null ? 0 : methodTools.length,
                toolNames(methodTools));
            AssistantMessage message = agent.call(userPrompt, config);
            String content = message == null ? "" : message.getText();
            if (content == null || content.isBlank()) {
                log.warn("[AI-AGENT] ReactAgent 返回空内容 agent={} threadId={} elapsedMs={}",
                    agentName, safeThreadId, System.currentTimeMillis() - startedAt);
                return Optional.empty();
            }
            log.info("[AI-AGENT] ReactAgent 调用成功 agent={} threadId={} responseLength={} elapsedMs={}",
                agentName, safeThreadId, content.length(), System.currentTimeMillis() - startedAt);
            return Optional.of(content.trim());
        } catch (Exception ex) {
            log.warn("[AI-AGENT] ReactAgent 调用失败 agent={} threadId={} elapsedMs={} reason={}",
                agentName,
                threadId == null || threadId.isBlank() ? agentName : threadId,
                System.currentTimeMillis() - startedAt,
                ex.getMessage());
            return Optional.empty();
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String toolNames(Object[] methodTools) {
        if (methodTools == null || methodTools.length == 0) {
            return "[]";
        }
        return Arrays.stream(methodTools)
            .map(tool -> tool == null ? "null" : tool.getClass().getSimpleName())
            .toList()
            .toString();
    }
}
