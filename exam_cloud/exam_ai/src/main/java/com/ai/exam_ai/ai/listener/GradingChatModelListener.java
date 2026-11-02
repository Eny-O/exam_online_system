package com.ai.exam_ai.ai.listener;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 批阅 LLM 交互观测：记录每次模型调用的输入消息、工具调用与输出结果，
 * 用于排查初评结果是否正确交接给复核 Agent。
 *
 * 由 Spring Boot Starter 自动注入到 OpenAiChatModel，无需手动配置。
 */
@Slf4j
@Component
public class GradingChatModelListener implements ChatModelListener {

    /** 观测开关 */
    @Value("${exam.ai.observability.enabled:true}")
    private boolean enabled;

    /** 单条消息与输出的打印长度上限 */
    @Value("${exam.ai.observability.max-message-length:1500}")
    private int maxMessageLength;

    /** 是否打印完整消息历史；关闭时只打印本次新增的消息，工具调用前后的数据流转更清晰 */
    @Value("${exam.ai.observability.print-full-history:false}")
    private boolean fullHistory;

    private final AtomicInteger counter = new AtomicInteger();
    private final ThreadLocal<Integer> currentSeq = new ThreadLocal<>();
    private final ThreadLocal<Integer> lastMessageCount = new ThreadLocal<>();

    @Override
    public void onRequest(ChatModelRequestContext context) {
        if (!enabled) {
            return;
        }
        ChatRequest request = context.chatRequest();
        int seq = counter.incrementAndGet();
        currentSeq.set(seq);
        List<ChatMessage> messages = request.messages();
        log.info("[LLM-{}] 请求 model={} messages={} tools=[{}]", seq, modelNameOf(request),
                messages.size(), toolNamesOf(request));
        int start = historyStart(messages.size());
        for (int i = start; i < messages.size(); i++) {
            log.info("[LLM-{}]   in[{}] {}: {}", seq, i, messages.get(i).type(), truncate(textOf(messages.get(i))));
        }
    }

    /**
     * 计算本次需要打印的消息起始下标：工具调用循环中历史以追加方式增长，
     * 因此只打印新增部分；消息数未增长说明是新一轮对话，重新打印全部。
     */
    private int historyStart(int messageCount) {
        Integer previous = lastMessageCount.get();
        int start = (fullHistory || previous == null || messageCount <= previous) ? 0 : previous;
        lastMessageCount.set(messageCount);
        return start;
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        if (!enabled) {
            return;
        }
        ChatResponse response = context.chatResponse();
        Integer seq = currentSeq.get();
        log.info("[LLM-{}] 响应 model={} finishReason={} tokenUsage={}", seq, response.modelName(),
                response.finishReason(), response.tokenUsage());
        String thinking = response.aiMessage().thinking();
        if (thinking != null && !thinking.isBlank()) {
            log.info("[LLM-{}]   思考 {}", seq, truncate(thinking));
        }
        for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
            log.info("[LLM-{}]   工具调用 {} args={}", seq, toolRequest.name(), toolRequest.arguments());
        }
        if (!response.aiMessage().hasToolExecutionRequests()) {
            log.info("[LLM-{}]   输出 {}", seq, truncate(response.aiMessage().text()));
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        log.error("[LLM] 调用失败 {}: {}", context.error().getClass().getName(),
                context.error().getMessage(), context.error());
    }

    private String modelNameOf(ChatRequest request) {
        return request.parameters() == null ? "unknown" : request.parameters().modelName();
    }

    private String toolNamesOf(ChatRequest request) {
        return request.toolSpecifications().stream()
                .map(spec -> spec.name())
                .collect(Collectors.joining(","));
    }

    /** 取消息文本：不同消息类型取值方法不同 */
    private String textOf(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.hasSingleText() ? userMessage.singleText() : userMessage.toString();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            return toolMessage.text();
        }
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null) {
            return "NULL";
        }
        if (text.length() <= maxMessageLength) {
            return text;
        }
        return text.substring(0, maxMessageLength) + "...(truncated)";
    }
}
