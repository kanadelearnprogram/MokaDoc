package com.kanade.backend.constant;

import lombok.Getter;

/**
 * SSE消息类型枚举
 */
@Getter
public enum SseMessageTypeEnum {
    
    /**
     * 流式内容片段
     */
    STREAMING("streaming", "流式内容"),
    
    /**
     * 完成标记
     */
    COMPLETE("complete", "完成"),
    
    /**
     * 错误信息
     */
    ERROR("error", "错误");
    
    private final String value;
    private final String description;
    
    SseMessageTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
