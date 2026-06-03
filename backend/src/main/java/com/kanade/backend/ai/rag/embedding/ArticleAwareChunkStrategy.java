package com.kanade.backend.ai.rag.embedding;

import com.kanade.backend.document.DocumentChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章边界对齐分块策略——Decorator 模式。
 * <p>
 * Phase 2: 在基础分块策略之前，检测中文法律/规章文档中的"第X条/章/节"边界，
 * 以此预分割文本，确保文章编号不会被固定尺寸分块切断。
 * </p>
 *
 * <p>核心逻辑：</p>
 * <ol>
 *   <li>检测文本中所有 {@code 第X条/章/节/编/部分} 模式的位置</li>
 *   <li>以这些位置为分割点预切分文本</li>
 *   <li>每个预切分段以文章编号开头，不会被切断</li>
 *   <li>若预切分段未超过阈值则作为单块，否则降级给基础策略处理</li>
 * </ol>
 *
 * @author kanade
 */
@Slf4j
public class ArticleAwareChunkStrategy implements ChunkingStrategy {

    /**
     * 中文法律文书结构标记：第X条/章/节/编/部分/项
     * 数字部分支持：一二三四五六七八九十百千零 + 阿拉伯数字 + 大写壹贰叁肆伍陆柒捌玖拾
     */
    private static final Pattern ARTICLE_HEADING =
            Pattern.compile("第[一二三四五六七八九十百千零\\d零壹贰叁肆伍陆柒捌玖拾佰仟]+[条节章编]");

    /** 单个文章段最大长度（基础策略块大小 × MERGE_FACTOR），超出则委托基础策略拆分 */
    private static final int MERGE_FACTOR = 2;

    private final ChunkingStrategy baseStrategy;

    public ArticleAwareChunkStrategy(ChunkingStrategy baseStrategy) {
        this.baseStrategy = baseStrategy;
    }

    @Override
    public List<DocumentChunk> chunk(String text, int pageNum) {
        if (text == null || text.isBlank()) {
            return baseStrategy.chunk(text, pageNum);
        }

        // 1. 定位所有文章边界
        List<Integer> boundaries = findArticleBoundaries(text);

        if (boundaries.isEmpty()) {
            // 无文章结构 → 原样委托
            return baseStrategy.chunk(text, pageNum);
        }

        // 2. 按文章边界预分割
        List<String> segments = splitByBoundaries(text, boundaries);

        log.info("  [文章边界] 定位到 {} 个文章标记, 预分割为 {} 段, 基础块大小={}",
                boundaries.size(), segments.size(), baseStrategy.getChunkSize());

        // 3. 合并连续短段 + 长段委托
        int maxMerge = baseStrategy.getChunkSize() * MERGE_FACTOR;
        int targetSize = baseStrategy.getChunkSize();
        List<DocumentChunk> result = new ArrayList<>();
        List<String> accumulator = new ArrayList<>();
        int accLen = 0;

        for (String segment : segments) {
            if (segment.length() > maxMerge) {
                // 超长文章段 → 先排空累积器，再委托基础策略拆分
                flushAccumulator(accumulator, accLen, pageNum, result);
                accumulator.clear();
                accLen = 0;
                log.debug("  [文章边界] 段 {} 字超过合并阈值 {}, 委托基础策略拆分",
                        segment.length(), maxMerge);
                result.addAll(baseStrategy.chunk(segment, pageNum));
                continue;
            }

            // 短段：判断是否放入累积器
            if (accumulator.isEmpty()) {
                // 起始一个新累积块
                accumulator.add(segment);
                accLen = segment.length();
            } else if (accLen + segment.length() <= targetSize) {
                // 追加后仍未超过目标大小 → 继续累积
                accumulator.add(segment);
                accLen += segment.length();
            } else {
                // 追加后会超过目标大小 → 先排空当前累积器，再从此段开始新累积
                flushAccumulator(accumulator, accLen, pageNum, result);
                accumulator.clear();
                accumulator.add(segment);
                accLen = segment.length();
            }
        }
        // 尾部排空
        flushAccumulator(accumulator, accLen, pageNum, result);

        if (log.isDebugEnabled()) {
            log.debug("  [文章边界] 分块结果: {} 块 (文章分割前 {} 段, 目标合并大小={})",
                    result.size(), segments.size(), targetSize);
        }

        return result;
    }

    /**
     * 定位所有文章标题的起始字符位置。
     */
    private List<Integer> findArticleBoundaries(String text) {
        List<Integer> boundaries = new ArrayList<>();
        Matcher matcher = ARTICLE_HEADING.matcher(text);
        while (matcher.find()) {
            boundaries.add(matcher.start());
        }

        // 排除第一个匹配（如果它就在文本开头，无需分割）
        if (!boundaries.isEmpty() && boundaries.get(0) == 0) {
            boundaries.remove(0);
        }

        return boundaries;
    }

    /**
     * 按边界位置将文本分割为若干段，每段以文章标题开头（第一段可能为前言）。
     */
    private List<String> splitByBoundaries(String text, List<Integer> boundaries) {
        List<String> segments = new ArrayList<>();
        int prev = 0;

        for (int boundary : boundaries) {
            if (boundary > prev) {
                String seg = text.substring(prev, boundary);
                if (!seg.isBlank()) {
                    segments.add(seg);
                }
            }
            prev = boundary;
        }

        // 尾部
        if (prev < text.length()) {
            String tail = text.substring(prev);
            if (!tail.isBlank()) {
                segments.add(tail);
            }
        }

        return segments;
    }

    /**
     * 将累积器中的多个短段合并为一个 chunk 输出。
     */
    private void flushAccumulator(List<String> accumulator, int accLen, int pageNum,
                                  List<DocumentChunk> result) {
        if (accumulator.isEmpty()) return;
        String merged = String.join("", accumulator);
        result.add(DocumentChunk.builder()
                .content(merged.trim())
                .pageNum(pageNum)
                .build());
    }

    @Override
    public int getChunkSize() {
        return baseStrategy.getChunkSize();
    }

    @Override
    public String getName() {
        return "article-aware+" + baseStrategy.getName();
    }
}
