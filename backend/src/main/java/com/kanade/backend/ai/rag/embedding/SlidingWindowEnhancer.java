package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 滑动窗口上下文增强器——Decorator 模式的具体装饰器。
 * <p>
 * 在 Embedding 前将相邻块的头尾边界句拼入当前块文本：
 * <pre>
 * 【上文 50 字】 + 当前块 1200 字 + 【下文 50 字】
 * </pre>
 * 让向量表征感知到块外的语义环境，减轻固定分块对语义流的切断。
 * </p>
 *
 * @author kanade
 */
@Slf4j
public class SlidingWindowEnhancer implements TextEnhancer {

    /** 前后文边界字符数 */
    private static final int BOUNDARY_SIZE = 50;

    @Override
    public String enhance(DocumentChunk chunk, List<DocumentChunk> allChunks) {
        if (allChunks == null || allChunks.size() <= 1) {
            return chunk.getContent();
        }

        int idx = chunk.getChunkIndex() != null ? chunk.getChunkIndex() : -1;
        if (idx < 0 || idx >= allChunks.size()) {
            return chunk.getContent();
        }

        String prevTail = extractPrevTail(allChunks, idx);
        String nextHead = extractNextHead(allChunks, idx);

        if (prevTail.isEmpty() && nextHead.isEmpty()) {
            return chunk.getContent();
        }

        StringBuilder sb = new StringBuilder();
        if (!prevTail.isEmpty()) {
            sb.append("【上文】").append(prevTail).append("\n");
        }
        sb.append(chunk.getContent());
        if (!nextHead.isEmpty()) {
            sb.append("\n【下文】").append(nextHead);
        }

        String enhanced = sb.toString();
        log.info("  [滑动窗口] Chunk[{}]: {}({}字) → {}字 (prev={}字, next={}字)",
                idx, chunk.getContent().substring(0, Math.min(20, chunk.getContent().length())) + "...",
                chunk.getContent().length(), enhanced.length(),
                prevTail.length(), nextHead.length());

        return enhanced;
    }

    /**
     * 提取前一个块的尾部 BOUNDARY_SIZE 字符。
     */
    private String extractPrevTail(List<DocumentChunk> allChunks, int idx) {
        if (idx <= 0) return "";
        String prev = allChunks.get(idx - 1).getContent();
        if (prev == null || prev.isEmpty()) return "";
        return prev.substring(Math.max(0, prev.length() - BOUNDARY_SIZE));
    }

    /**
     * 提取后一个块的头部 BOUNDARY_SIZE 字符。
     */
    private String extractNextHead(List<DocumentChunk> allChunks, int idx) {
        if (idx >= allChunks.size() - 1) return "";
        String next = allChunks.get(idx + 1).getContent();
        if (next == null || next.isEmpty()) return "";
        return next.substring(0, Math.min(BOUNDARY_SIZE, next.length()));
    }
}
