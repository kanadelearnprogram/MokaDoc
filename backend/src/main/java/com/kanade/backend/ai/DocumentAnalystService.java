package com.kanade.backend.ai;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * 文档分析专家AI服务
 * 用于文档内容分析、总结、提取关键信息
 */
public interface DocumentAnalystService {
    
    /**
     * 分析文档内容
     *
     * @param documentContent 文档内容
     * @return 流式返回分析结果
     */
    @SystemMessage("""
        你是一位专业的文档分析专家。请仔细分析用户提供的文档内容，并给出：
        1. 核心主题和要点
        2. 关键信息提取
        3. 逻辑结构分析
        
        请用清晰、简洁的语言回答。
        """)
    Flux<String> analyze(String documentContent);
    
    /**
     * 总结文档
     *
     * @param documentContent 文档内容
     * @param maxLength       最大长度（字数）
     * @return 流式返回总结结果
     */
    @SystemMessage("""
        你是一位专业的文档总结专家。请将用户提供的文档内容总结为{{maxLength}}字以内的精炼摘要。
        要求：
        - 保留核心观点和关键信息
        - 语言简洁流畅
        - 逻辑清晰
        """)
    Flux<String> summarize(String documentContent, Integer maxLength);
}
