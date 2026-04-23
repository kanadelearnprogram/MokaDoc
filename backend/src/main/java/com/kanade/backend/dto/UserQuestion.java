package com.kanade.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户问题请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestion {
    /**
     * 问题内容
     */
    private String content;
    
    /**
     * 会话ID(可选,为空则创建新会话)
     */
    private Long sessionId;
}
