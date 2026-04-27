package com.kanade.backend.service;

import com.kanade.backend.entity.QaMessage;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.kanade.backend.entity.QaSession;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答会话表 服务层。
 *
 * @author kanade
 */
public interface QaSessionService extends IService<QaSession> {

    /**
     * 创建新会话
     * @param userId 用户ID
     * @param sessionName 会话名称(可选,为空则自动生成)
     * @return 会话ID
     */
    Long createSession(Long userId, String sessionName);

    /**
     * 创建新会话(返回完整实体)
     */
    QaSession createSessionWithDetails(Long userId, String sessionName, String summary);

    /**
     * 更新会话信息
     */
    QaSession updateSessionInfo(Long sessionId, Long userId, String sessionName, String summary);

    /**
     * 获取用户的会话列表
     * @param userId 用户ID
     * @return 会话列表(按更新时间倒序)
     */
    List<QaSession> listUserSessions(Long userId);

    /**
     * 在会话中发送消息(包含保存和AI响应)
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param documentIds 参考文档ID列表(可选,用于RAG)
     * @return 流式AI响应
     */
    Flux<String> sendMessage(Long sessionId, String userMessage, List<Long> documentIds);

    /**
     * 删除会话(含级联清理消息)
     * @param sessionId 会话ID
     * @param userId 用户ID(权限校验)
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 构建连接成功数据JSON
     * @param sessionId 会话ID
     * @return JSON字符串
     */
    String buildConnectedData(Long sessionId);


    Page<QaSession> listAppChatHistoryByPage(Long id, int pageSize, LocalDateTime lastCreateTime, HttpServletRequest request);



}
