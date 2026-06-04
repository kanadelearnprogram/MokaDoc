package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 文本增强责任链——Chain of Responsibility 模式。
 * <p>
 * 串联多个 {@link TextEnhancer} 依次对 Chunk 文本加工，
 * 前一个增强器的输出作为下一个的输入。
 * 任一增强器抛出异常时自动跳过，不影响整条链。
 * </p>
 *
 * @author kanade
 */
@Slf4j
public class TextEnhancerChain implements TextEnhancer {

    private final List<TextEnhancer> enhancers;

    public TextEnhancerChain(List<TextEnhancer> enhancers) {
        this.enhancers = enhancers != null ? enhancers : List.of();
    }

    @Override
    public String enhance(DocumentChunk chunk, List<DocumentChunk> allChunks) {
        if (enhancers.isEmpty()) {
            return chunk.getContent();
        }

        // 逐步增强：每个 enhancer 在前一个输出上加工
        String enhanced = chunk.getContent();
        for (int i = 0; i < enhancers.size(); i++) {
            try {
                DocumentChunk temp = DocumentChunk.builder()
                        .content(enhanced)
                        .pageNum(chunk.getPageNum())
                        .chunkIndex(chunk.getChunkIndex())
                        .build();
                enhanced = enhancers.get(i).enhance(temp, allChunks);
            } catch (Exception e) {
                log.warn("[增强链] 第 {} 个增强器失败，跳过: {}", i, e.getMessage());
            }
        }

        if (!enhanced.equals(chunk.getContent())) {
            log.debug("[增强链] 增强完成: {} 字 → {} 字",
                    chunk.getContent().length(), enhanced.length());
        }
        return enhanced;
    }
}
