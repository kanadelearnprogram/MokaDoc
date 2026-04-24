package com.kanade.backend.config;

import com.kanade.backend.ai.memory.PersistenceMemory;
import com.kanade.backend.service.QaMessageService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j ChatMemory配置
 * 提供基于数据库持久化的记忆提供者
 */
@Slf4j
@Configuration
public class ChatMemoryConfig {

    @Resource
    private QaMessageService qaMessageService;

    /**
     * 默认滑动窗口大小：保留最近20条消息
     * 可根据实际需求调整（建议10-50之间）
     */
    private static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * 注册ChatMemoryProvider Bean
     * LangChain4j会自动注入到使用@AiService的接口中
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                // memoryId就是sessionId
                Long sessionId = (Long) memoryId;
                
                log.debug("🔑 [获取记忆] sessionId={}", sessionId);
                
                // 为每个sessionId创建独立的PersistenceMemory实例
                return new PersistenceMemory(sessionId, qaMessageService, DEFAULT_MAX_MESSAGES);
            }
        };
    }
}
