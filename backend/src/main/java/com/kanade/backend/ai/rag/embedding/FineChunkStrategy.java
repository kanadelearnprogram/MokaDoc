package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 细粒度分块策略——Strategy 模式的具体策略。
 * <p>
 * 较小分块（默认 200 字 + 30 字重叠），适合精排阶段，
 * 保证高精度和精确定位。
 * </p>
 *
 * @author kanade
 */
@Slf4j
public class FineChunkStrategy implements ChunkingStrategy {

    public static final int DEFAULT_SIZE = 200;
    public static final int DEFAULT_OVERLAP = 30;

    private final int chunkSize;
    private final int chunkOverlap;

    public FineChunkStrategy() {
        this(DEFAULT_SIZE, DEFAULT_OVERLAP);
    }

    public FineChunkStrategy(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    public List<DocumentChunk> chunk(String text, int pageNum) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String clean = text.replace("\r", "").trim();
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(start + chunkSize, clean.length());
            String content = clean.substring(start, end).trim();
            if (!content.isBlank()) {
                chunks.add(DocumentChunk.builder()
                        .content(content)
                        .pageNum(pageNum)
                        .charOffsetStart(start)
                        .build());
            }
            if (end >= clean.length()) break;
            start = Math.max(0, end - chunkOverlap);
        }

        return chunks;
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public String getName() {
        return "fine";
    }
}
