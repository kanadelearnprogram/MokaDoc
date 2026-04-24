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
 * 基于数据库持久化的聊天记忆实现
 * 从sys_qa_message表加载历史消息，实现多轮对话上下文
 */
@Slf4j
public class PersistenceMemory implements ChatMemory {

    private final Long sessionId;
    private final QaMessageService messageService;
    private final int maxMessages; // 滑动窗口大小

    public PersistenceMemory(Long sessionId, QaMessageService messageService, int maxMessages) {
        this.sessionId = sessionId;
        this.messageService = messageService;
        this.maxMessages = maxMessages;
        log.debug("🧠 [初始化记忆] sessionId={}, maxMessages={}", sessionId, maxMessages);
    }

    @Override
    public Object id() {
        return sessionId;
    }

    /**
     * 添加消息到记忆（实际已由sendMessage保存到数据库，这里只需记录日志）
     */
    @Override
    public void add(ChatMessage chatMessage) {
        log.debug("💭 [添加消息到记忆] sessionId={}, messageType={}", 
                  sessionId, 
                  chatMessage instanceof UserMessage ? "user" : "ai");
        // 注意：消息的实际保存已在QaSessionServiceImpl.sendMessage中完成
        // 这里不需要重复保存，LangChain4j调用此方法只是为了更新内存中的状态
    }

    /**
     * 从数据库加载会话历史消息（滑动窗口策略）
     */
    @Override
    public List<ChatMessage> messages() {
        // 1. 从数据库查询该会话的最近N条消息
        List<QaMessage> dbMessages = messageService.listRecentMessages(sessionId, maxMessages);
        
        if (dbMessages.isEmpty()) {
            log.debug("📭 [无历史记录] sessionId={}", sessionId);
            return new ArrayList<>();
        }

        // 2. 转换为LangChain4j的ChatMessage格式
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

        log.debug("📚 [加载记忆] sessionId={}, messageCount={}", sessionId, chatMessages.size());
        
        return chatMessages;
    }

    /**
     * 清空记忆（逻辑删除该会话的所有消息）
     */
    @Override
    public void clear() {
        log.info("🗑️ [清空记忆] sessionId={}", sessionId);
        messageService.deleteBySessionId(sessionId);
    }
}
