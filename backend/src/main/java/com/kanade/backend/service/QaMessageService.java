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
}
