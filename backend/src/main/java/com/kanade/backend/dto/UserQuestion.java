package com.kanade.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /**
     * 文档ID列表(可选,用于RAG参考文档)
     */
    private List<Long> documentIds;
}
