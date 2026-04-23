package com.kanade.backend.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kanade.backend.constant.ChatConstant.SSE_RECONNECT_TIME_MS;
import static com.kanade.backend.constant.ChatConstant.SSE_TIMEOUT_MS;

@Component
@Slf4j
public class SseEmitterManager {

    /**
     * 存储所有的 SseEmitter
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建 SseEmitter
     *
     * @param taskId 任务ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("⏰ [SSE超时] taskId={}", taskId);
            emitterMap.remove(taskId);
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            log.debug("🔚 [SSE完成] taskId={}", taskId);
            emitterMap.remove(taskId);
        });

        // 设置错误回调
        emitter.onError((e) -> {
            log.error("💥 [SSE错误] taskId={}, error={}", taskId, e.getMessage());
            emitterMap.remove(taskId);
        });

        emitterMap.put(taskId, emitter);
        log.debug("🔌 [SSE创建] taskId={}", taskId);

        return emitter;
    }

    /**
     * 发送消息
     *
     * @param taskId  任务ID
     * @param message 消息内容
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(message)
                    .reconnectTime(SSE_RECONNECT_TIME_MS));
            log.trace("📤 [SSE发送] taskId={}, messageLength={}", taskId, message.length());
        } catch (IOException e) {
            // 客户端断开连接，静默处理
            log.debug("❌ [SSE发送失败] taskId={}, reason=客户端已断开", taskId);
            emitterMap.remove(taskId);
        }
    }

    /**
     * 完成连接
     *
     * @param taskId 任务ID
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.complete();
            log.debug("✅ [SSE关闭] taskId={}", taskId);
        } catch (Exception e) {
            log.error("❌ [SSE关闭失败] taskId={}, error={}", taskId, e.getMessage());
        } finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 检查 Emitter 是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }
}

