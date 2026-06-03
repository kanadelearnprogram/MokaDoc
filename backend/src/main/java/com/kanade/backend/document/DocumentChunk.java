package com.kanade.backend.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    /** 原始文本内容 */
    private String content;

    /** 页码（PDF 分页时有效） */
    private Integer pageNum;

    /** 块索引（文档内从 0 开始） */
    private Integer chunkIndex;

    /**
     * 增强后的文本（仅用于 Embedding，不会被存储或展示）。
     * 由 {@link com.kanade.backend.ai.rag.embedding.TextEnhancerChain} 填充，
     * 在 EmbeddingModel 调用时替代 content 作为输入。
     */
    private String enhancedText;

    /**
     * 前一个块的尾部边界文本（用于滑动窗口上下文增强）。
     */
    private String prevTail;

    /**
     * 后一个块的头部边界文本（用于滑动窗口上下文增强）。
     */
    private String nextHead;

    /**
     * 分块粒度标识：coarse / fine。
     * 双层索引时区分粗粒度块和细粒度块。
     */
    private String granularity;

    /**
     * 字符偏移量（在原始文本中的起始位置）。
     * 用于 Phase 4 Parent-Child 分块时建立父子块映射关系。
     */
    private Integer charOffsetStart;

    /**
     * 父块索引（Parent-Child 分块时子块指向父块）。
     * 子块的 parentChunkIndex = 父块的 chunkIndex。
     * 检索时若命中子块，通过此索引取出父块内容提供上下文。
     */
    private Integer parentChunkIndex;
}
