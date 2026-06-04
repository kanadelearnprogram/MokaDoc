package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;

import java.util.List;

/**
 * 文本增强器——Strategy 模式。
 * <p>
 * 在文档 Chunk 进入 Embedding 之前对文本进行增强，
 * 使生成的向量携带更多上下文或结构化信息。
 * </p>
 *
 * @author kanade
 */
@FunctionalInterface
public interface TextEnhancer {

    /**
     * 对指定 chunk 的文本进行增强。
     *
     * @param chunk     当前要处理的 chunk
     * @param allChunks 同一文档的全部 chunk 列表（供滑动窗口等跨块策略使用）
     * @return 增强后的文本（仅用于 Embedding，不替换原始 content）
     */
    String enhance(DocumentChunk chunk, List<DocumentChunk> allChunks);
}
