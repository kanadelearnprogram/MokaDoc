package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;

import java.util.List;

/**
 * 分块策略接口——Strategy 模式。
 * <p>
 * 定义如何将一段文本按指定粒度切分为 Chunk，
 * 支持不同场景下的分块需求（粗粒度召回、细粒度精排等）。
 * </p>
 *
 * @author kanade
 */
@FunctionalInterface
public interface ChunkingStrategy {

    /**
     * 将文本按当前策略切分为 Chunk。
     *
     * @param text    待切分的纯文本
     * @param pageNum 页码（用于跨页分块时标记来源）
     * @return 切分后的 Chunk 列表
     */
    List<DocumentChunk> chunk(String text, int pageNum);

    /** 获取该策略的名称标识 */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * 获取该策略的块大小阈值。
     * 用于 Phase 1 PDF 页面感知分块判断"是否整页作为一个块"的参考值。
     */
    default int getChunkSize() {
        return Integer.MAX_VALUE;
    }
}
