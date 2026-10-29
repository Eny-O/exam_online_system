package com.ai.exam_ai.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 对话记忆配置：记忆介质为内存，不做持久化。
 */
@Configuration
public class LangChainMemoryConfig {

    /**
     * 内存记忆存储：按 memoryId 隔离会话消息，服务重启后清空。
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * 记忆提供者：为每个 memoryId 提供带窗口上限的对话记忆。
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore,
                                                 @Value("${exam.ai.memory.max-messages:20}") Integer maxMessages) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
