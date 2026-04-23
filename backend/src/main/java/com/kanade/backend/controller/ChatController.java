package com.kanade.backend.controller;

import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.dto.UserQuestion;
import com.kanade.backend.entity.QaSession;
import com.kanade.backend.entity.User;
import com.kanade.backend.service.QaSessionService;
import com.kanade.backend.service.UserService;
import com.kanade.backend.sse.SseEmitterManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
@Tag(name = "聊天管理", description = "提供流式聊天功能")
public class ChatController {

    @Resource
    private QaSessionService qaSessionService;

    @Resource
    private UserService userService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    /**
     * 智能问答接口(自动创建会话)
     * 前端直接发送问题,后端自动创建会话并返回sessionId
     * 通过SSE流式返回AI响应
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "智能问答(自动创建会话)")
    public SseEmitter ask(@RequestBody UserQuestion question,
                          HttpServletRequest request) {
        // 1. 获取当前用户
        User currentUser = userService.getLoginUser(request);
        
        // 2. 自动创建会话(如果未指定sessionId)
        Long sessionId = question.getSessionId();
        boolean isNewSession = false;
        if (sessionId == null) {
            // 创建新会话
            sessionId = qaSessionService.createSession(currentUser.getId(), null);
            isNewSession = true;
            log.info("📝 [新会话] sessionId={}, userId={}", sessionId, currentUser.getId());
        } else {
            // 验证会话归属权
            QaSession session = qaSessionService.getById(sessionId);
            if (session == null || !session.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("无权访问此会话");
            }
            log.debug("🔄 [复用会话] sessionId={}, userId={}", sessionId, currentUser.getId());
        }

        // 3. 创建 SSE 连接(使用 sessionId 作为 key)
        final Long finalSessionId = sessionId; // 声明为final供lambda使用
        SseEmitter emitter = sseEmitterManager.createEmitter(finalSessionId.toString());
        log.debug("🔌 [SSE连接] taskId={}", finalSessionId);

        try {
            // 4. 发送初始消息(包含sessionId, JSON格式)
            String connectedData = qaSessionService.buildConnectedData(finalSessionId);
            emitter.send(SseEmitter.event()
                .name("message")
                .data(connectedData));

            // 5. 调用服务层处理消息(返回Flux<String>,已经是JSON格式)
            log.debug("💬 [发送消息] sessionId={}, messageLength={}", finalSessionId, question.getContent().length());
            
            qaSessionService.sendMessage(finalSessionId, question.getContent())
                .doOnNext(jsonData -> {
                    try {
                        sseEmitterManager.send(finalSessionId.toString(), jsonData);
                    } catch (Exception e) {
                        log.warn("发送 SSE 消息失败: {}", e.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    sseEmitterManager.complete(finalSessionId.toString());
                    log.debug("✅ [响应完成] sessionId={}", finalSessionId);
                })
                .doOnError(error -> {
                    log.error("❌ [响应失败] sessionId={}, error={}", finalSessionId, error.getMessage());
                    sseEmitterManager.complete(finalSessionId.toString());
                })
                .subscribe();

        } catch (IOException e) {
            log.error("初始化 SSE 连接失败, sessionId={}", finalSessionId, e);
            sseEmitterManager.complete(finalSessionId.toString());
        }

        return emitter;
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/session/list")
    @Operation(summary = "获取会话列表")
    public BaseResponse<List<QaSession>> listSessions(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        List<QaSession> sessions = qaSessionService.listUserSessions(currentUser.getId());
        return ResultUtils.success(sessions);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话")
    public BaseResponse<Boolean> deleteSession(@PathVariable Long sessionId,
                                                HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        qaSessionService.deleteSession(sessionId, currentUser.getId());
        return ResultUtils.success(true);
    }
}
