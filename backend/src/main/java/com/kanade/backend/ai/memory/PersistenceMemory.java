package com.kanade.backend.ai.memory;

import com.kanade.backend.entity.QaMessage;
import com.kanade.backend.service.QaMessageService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Redis + 数据库双层缓存的聊天记忆实现
 * 
 * 架构设计：
 * 1. Redis作为L1缓存（高速读取，~0.1ms）
 * 2. 数据库作为L2持久化存储（可靠存储，~5-10ms）
 * 3. 滑动窗口策略：只保留最近N条消息
 * 
 * 工作流程：
 * - 读取：先查Redis → 未命中再查数据库 → 写入Redis
 * - 写入：保存到数据库 → 追加到Redis
 * - 清除：删除数据库记录 → 删除Redis缓存
 */
@Slf4j
public class PersistenceMemory implements ChatMemory {

    private final Long sessionId;
    private final QaMessageService messageService;
    private final RedisMemoryStore redisMemoryStore;
    private final int maxMessages; // 滑动窗口大小

    public PersistenceMemory(Long sessionId, QaMessageService messageService, 
                            RedisMemoryStore redisMemoryStore, int maxMessages) {
        this.sessionId = sessionId;
        this.messageService = messageService;
        this.redisMemoryStore = redisMemoryStore;
        this.maxMessages = maxMessages;
        log.debug("🧠 [初始化记忆] sessionId={}, maxMessages={}, cache=Redis+DB", sessionId, maxMessages);
    }

    @Override
    public Object id() {
        return sessionId;
    }

    /**
     * 添加消息到记忆
     * 
     * 注意：
     * 1. 消息的实际保存已在QaSessionServiceImpl.sendMessage中完成（写入数据库）
     * 2. 这里只需追加到Redis缓存，保持缓存与数据库同步
     */
    @Override
    public void add(ChatMessage chatMessage) {
        log.debug("💭 [添加消息到记忆] sessionId={}, messageType={}", 
                  sessionId, 
                  chatMessage instanceof UserMessage ? "user" : "ai");
        
        // 追加到Redis缓存（异步操作，失败不影响主流程）
        redisMemoryStore.appendMessage(sessionId, chatMessage);
    }

    /**
     * 从缓存层加载会话历史消息（Redis + 数据库双层策略）
     * 
     * 读取流程：
     * 1. 尝试从Redis读取（L1缓存，极速）
     * 2. 如果Redis未命中，从数据库查询（L2持久化）
     * 3. 将数据库结果写入Redis，下次可直接命中
     */
    @Override
    public List<ChatMessage> messages() {
        // ① 尝试从Redis获取（L1缓存）
        List<ChatMessage> cachedMessages = redisMemoryStore.getMessages(sessionId);
        if (cachedMessages != null) {
            // Redis命中，直接返回
            log.debug("⚡ [使用Redis缓存] sessionId={}, messageCount={}", 
                     sessionId, cachedMessages.size());
            return cachedMessages;
        }

        // ② Redis未命中，从数据库查询（L2持久化）
        log.debug("📊 [查询数据库] sessionId={}, limit={}", sessionId, maxMessages);
        List<QaMessage> dbMessages = messageService.listRecentMessages(sessionId, maxMessages);
        
        if (dbMessages.isEmpty()) {
            log.debug("📭 [无历史记录] sessionId={}", sessionId);
            return new ArrayList<>();
        }

        // ③ 转换为LangChain4j的ChatMessage格式
        List<ChatMessage> chatMessages = dbMessages.stream()
            .map(msg -> {
                if (msg.getMessageType() == 1) {
                    // 用户消息
                    return UserMessage.from(msg.getContent());
                } else {
                    // AI消息
                    return AiMessage.from(msg.getContent());
                }
            })
            .collect(Collectors.toList());

        // ④ 写入Redis缓存，下次请求可直接命中
        redisMemoryStore.saveMessages(sessionId, chatMessages);

        log.debug("📚 [加载记忆完成] sessionId={}, messageCount={}, source=DB→Redis", 
                 sessionId, chatMessages.size());
        
        return chatMessages;
    }

    /**
     * 清空记忆（同时删除数据库记录和Redis缓存）
     */
    @Override
    public void clear() {
        log.info("🗑️ [清空记忆] sessionId={}", sessionId);
        
        // ① 逻辑删除数据库中的消息
        messageService.deleteBySessionId(sessionId);
        
        // ② 删除Redis缓存
        redisMemoryStore.deleteMemory(sessionId);
        
        log.info("✅ [记忆清空完成] sessionId={}", sessionId);
    }
}
