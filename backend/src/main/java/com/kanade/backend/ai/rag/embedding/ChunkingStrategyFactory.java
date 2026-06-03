package com.kanade.backend.ai.rag.embedding;

import lombok.extern.slf4j.Slf4j;

/**
 * 分块策略工厂——Factory 模式。
 * <p>
 * 根据配置名称创建对应的 {@link ChunkingStrategy} 实例。
 * 支持 "coarse"（粗粒度）、"fine"（细粒度）和 "dual"（双策略）。
 * </p>
 *
 * @author kanade
 */
@Slf4j
public class ChunkingStrategyFactory {

    private ChunkingStrategyFactory() {
    }

    /**
     * 创建指定名称的分块策略。
     *
     * @param name       策略名称：coarse / fine / dual
     * @param coarseSize 粗粒度块大小
     * @param coarseOverlap 粗粒度重叠
     * @param fineSize   细粒度块大小
     * @param fineOverlap 细粒度重叠
     * @return 对应的分块策略
     */
    public static ChunkingStrategy create(String name,
                                           int coarseSize, int coarseOverlap,
                                           int fineSize, int fineOverlap) {
        if (name == null) name = "coarse";

        return switch (name.toLowerCase()) {
            case "coarse" -> {
                log.info("[分块策略] 粗粒度: size={}, overlap={}", coarseSize, coarseOverlap);
                yield new CoarseChunkStrategy(coarseSize, coarseOverlap);
            }
            case "fine" -> {
                log.info("[分块策略] 细粒度: size={}, overlap={}", fineSize, fineOverlap);
                yield new FineChunkStrategy(fineSize, fineOverlap);
            }
            case "dual" -> {
                log.info("[分块策略] 双层粒度: coarse({},{}), fine({},{})",
                        coarseSize, coarseOverlap, fineSize, fineOverlap);
                yield new CoarseChunkStrategy(coarseSize, coarseOverlap);
            }
            case "article-aware" -> {
                log.info("[分块策略] 文章边界对齐(粗粒度): base=coarse({},{}), 合并阈值={}",
                        coarseSize, coarseOverlap, coarseSize * 2);
                yield new ArticleAwareChunkStrategy(new CoarseChunkStrategy(coarseSize, coarseOverlap));
            }
            default -> {
                log.warn("[分块策略] 未知策略: {}, 使用粗粒度", name);
                yield new CoarseChunkStrategy();
            }
        };
    }

    /**
     * 使用默认参数创建。
     */
    public static ChunkingStrategy create(String name) {
        return create(name,
                CoarseChunkStrategy.DEFAULT_SIZE, CoarseChunkStrategy.DEFAULT_OVERLAP,
                FineChunkStrategy.DEFAULT_SIZE, FineChunkStrategy.DEFAULT_OVERLAP);
    }

    /**
     * 判断是否为双层策略。
     */
    public static boolean isDual(String name) {
        return "dual".equalsIgnoreCase(name);
    }
}
