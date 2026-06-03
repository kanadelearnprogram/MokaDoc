package com.kanade.backend.ai.rag.embedding;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.kanade.backend.document.DocumentChunk;
import com.kanade.backend.graph.GraphCrudService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图增强实体感知增强器——Strategy 模式的具体策略。
 * <p>
 * 在 Embedding 前利用 HanLP 分词从 Chunk 文本中提取候选实体，
 * 查询 {@link GraphCrudService#expandSubgraph} 获取已有图关系，
 * 将结构化知识拼入文本前缀后再调用 EmbeddingModel。
 * </p>
 *
 * <p>核心思想：让向量本身携带结构化知识。传统 Graph RAG 在图和向量两条独立管道
 * 检索后用 RRF 融合，本增强器将图关系信息注入到向量生成的输入层，
 * 使检索时 Query 向量与含图信息的 Chunk 向量在语义空间中对齐。</p>
 *
 * @author kanade
 */
@Slf4j
public class GraphEntityEnhancer implements TextEnhancer {

    private final GraphCrudService graphCrudService;
    private final Long userId;

    /** 实体候选最大字数 */
    private static final int MAX_ENTITY_LENGTH = 20;
    /** 最小实体字数 */
    private static final int MIN_ENTITY_LENGTH = 2;

    public GraphEntityEnhancer(GraphCrudService graphCrudService, Long userId) {
        this.graphCrudService = graphCrudService;
        this.userId = userId;
    }

    @Override
    public String enhance(DocumentChunk chunk, List<DocumentChunk> allChunks) {
        String text = chunk.getContent();
        if (text == null || text.isBlank()) return text;

        try {
            // 1. HanLP 分词 + 词性标注，提取候选实体
            List<Term> terms = HanLP.segment(text);
            Set<String> candidates = extractCandidateEntities(terms);

            if (candidates.isEmpty()) {
                log.debug("  [图增强] Chunk[{}]: 未提取到候选实体", chunk.getChunkIndex());
                return text;
            }

            log.info("  [图增强] Chunk[{}]: HanLP 提取候选实体 {} 个: {}",
                    chunk.getChunkIndex(), candidates.size(),
                    String.join(", ", candidates));

            // 2. 对候选实体查询 MySQL 图谱
            List<String> graphPieces = new ArrayList<>();
            int hitCount = 0;
            for (String entity : candidates) {
                Map<String, Object> subgraph = graphCrudService.expandSubgraph(entity, userId, 1);
                if (subgraph != null && !subgraph.isEmpty()) {
                    String formatted = formatGraphText(entity, subgraph);
                    if (formatted != null) {
                        graphPieces.add(formatted);
                        hitCount++;
                    }
                }
            }

            // 3. 拼接图信息到文本前缀
            if (graphPieces.isEmpty()) {
                log.info("  [图增强] Chunk[{}]: 0/{} 候选实体在图库中命中，跳过",
                        chunk.getChunkIndex(), candidates.size());
                return text;
            }

            String prefix = "【实体关系】" + String.join("; ", graphPieces);
            log.info("  [图增强] Chunk[{}]: {} 个实体在图库命中 → 注入关系文本({}字)",
                    chunk.getChunkIndex(), hitCount, prefix.length());

            return prefix + " | " + text;

        } catch (Exception e) {
            log.debug("  [图增强] Chunk[{}]: 增强失败，降级返回原文: {}",
                    chunk.getChunkIndex(), e.getMessage());
            return text;
        }
    }

    /**
     * 从 HanLP 分词结果中提取候选实体。
     * 策略：取名词(n) / 动词(v) / 专有名词(ns,nr,nt) 且长度在 [2,20] 的词语。
     */
    private Set<String> extractCandidateEntities(List<Term> terms) {
        Set<String> candidates = new LinkedHashSet<>();

        for (Term term : terms) {
            String word = term.word;
            String nature = term.nature != null ? term.nature.toString() : "";

            if (word.length() < MIN_ENTITY_LENGTH || word.length() > MAX_ENTITY_LENGTH) continue;
            // 过滤停用词
            if (isStopWord(word)) continue;

            // 名词、动词、专有名词作为候选实体
            if (nature.startsWith("n") || nature.startsWith("v")
                || nature.startsWith("ns") || nature.startsWith("nr")
                || nature.startsWith("nt")) {
                candidates.add(word);
            }
        }
        return candidates;
    }

    private boolean isStopWord(String word) {
        return Set.of("可以", "一个", "以及", "如果", "因为", "所以", "进行",
                "使用", "通过", "包括", "没有", "不是", "但是", "这个", "那个",
                "什么", "怎么", "如何", "为什么", "哪些", "是否", "可能", "已经",
                "the", "and", "for", "with", "this", "that", "from", "have")
                .contains(word.toLowerCase());
    }

    /**
     * 将子图结果格式化为可读文本。
     */
    private String formatGraphText(String entityName, Map<String, Object> subgraph) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>)
                subgraph.getOrDefault("nodes", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>)
                subgraph.getOrDefault("edges", List.of());

        if (nodes.isEmpty() && edges.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(entityName);

        // 关联实体列表（排除自身）
        List<String> relatedNames = nodes.stream()
                .map(n -> String.valueOf(n.get("name")))
                .filter(name -> !name.equals(entityName))
                .distinct()
                .toList();
        if (!relatedNames.isEmpty()) {
            sb.append(" → [").append(String.join(", ", relatedNames)).append("]");
        }

        // 关系类型
        if (!edges.isEmpty()) {
            Set<String> types = new LinkedHashSet<>();
            for (Map<String, Object> edge : edges) {
                Object t = edge.get("type");
                if (t != null) types.add(t.toString());
            }
            if (!types.isEmpty()) {
                sb.append("(").append(String.join(", ", types)).append(")");
            }
        }

        return sb.toString();
    }
}
