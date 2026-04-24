package com.kanade.backend.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.kanade.backend.entity.QaMessage;

import java.time.LocalDateTime;

/**
 * 会话消息表 服务层。
 *
 * @author kanade
 */
public interface QaMessageService extends IService<QaMessage> {

    Page<QaMessage> listSessionChatByPage(Long sessionId, int pageSize, LocalDateTime lastCreateTime, Long id);

    /**
     * 查询会话的最近N条消息（用于加载聊天记忆）
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 消息列表（按时间升序）
     */
    java.util.List<QaMessage> listRecentMessages(Long sessionId, int limit);

    /**
     * 删除会话的所有消息（逻辑删除）
     * @param sessionId 会话ID
     */
    void deleteBySessionId(Long sessionId);
}
