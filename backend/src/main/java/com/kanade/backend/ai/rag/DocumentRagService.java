package com.kanade.backend.ai.rag;

import com.kanade.backend.ai.rag.embedding.ChunkingStrategy;
import com.kanade.backend.ai.rag.embedding.ChunkingStrategyFactory;
import com.kanade.backend.ai.rag.embedding.FineChunkStrategy;
import com.kanade.backend.ai.rag.embedding.GraphEntityEnhancer;
import com.kanade.backend.ai.rag.embedding.SlidingWindowEnhancer;
import com.kanade.backend.ai.rag.embedding.TextEnhancer;
import com.kanade.backend.ai.rag.embedding.TextEnhancerChain;
import com.kanade.backend.document.DocumentChunk;
import com.kanade.backend.document.DocumentParseService;
import com.kanade.backend.entity.Document;
import com.kanade.backend.graph.GraphCrudService;
import com.kanade.backend.service.DocumentService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DocumentRagService {

    @Value("${file.upload.dir:./uploads/documents}")
    private String uploadDir;

    // ====== 增强链配置 ======

    @Value("${rag.embedding.enhancers:}")
    private String enhancersConfig;

    @Value("${rag.embedding.chunk-strategy:coarse}")
    private String chunkStrategyName;

    @Value("${rag.embedding.chunk.coarse-size:800}")
    private int coarseChunkSize;

    @Value("${rag.embedding.chunk.coarse-overlap:100}")
    private int coarseChunkOverlap;

    @Value("${rag.embedding.chunk.fine-size:200}")
    private int fineChunkSize;

    @Value("${rag.embedding.chunk.fine-overlap:30}")
    private int fineChunkOverlap;

    @Value("${rag.embedding.signature-cache.enabled:true}")
    private boolean signatureCacheEnabled;

    // ====== Phase 3: 邻居块扩展配置 ======

    @Value("${rag.embedding.neighbor-expansion.enabled:true}")
    private boolean neighborExpansionEnabled;

    @Value("${rag.embedding.neighbor-expansion.window:1}")
    private int neighborWindow;

    // ====== Phase 4: Parent-Child 分块配置 ======

    @Value("${rag.embedding.parent-child.enabled:true}")
    private boolean parentChildEnabled;

    @Value("${elasticsearch.index-name:rag_documents}")
    private String esIndexName;

    @Value("${rag.es.max-results:10}")
    private int esMaxResults;

    @Value("${rag.es.min-score:0}")
    private double esMinScore;

    @Resource
    @Lazy
    private DocumentService documentService;

    @Resource
    private ElasticsearchEmbeddingStore embeddingStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private DocumentParseService documentParseService;

    @Autowired(required = false)
    private GraphCrudService graphCrudService;

    @Resource
    private ElasticsearchClient esClient;

    private TextEnhancerChain enhancerChain;
    private ChunkingStrategy chunkingStrategy;

    /** 会话缓存：sessionId → 已关联的文档 ID 集合 */
    private final Map<Long, Set<Long>> sessionDocumentIds = new ConcurrentHashMap<>();

    /** 已索引的文档 ID */
    private final Set<Long> indexedDocuments = ConcurrentHashMap.newKeySet();

    /** 内容签名缓存：documentId → SHA256 签名 */
    private final Map<Long, String> documentSignatureCache = new ConcurrentHashMap<>();

    private static final int LOCAL_FALLBACK_MAX_RESULTS = 8;

    @PostConstruct
    public void init() {
        // 1. 初始化分块策略
        this.chunkingStrategy = ChunkingStrategyFactory.create(
                chunkStrategyName, coarseChunkSize, coarseChunkOverlap,
                fineChunkSize, fineChunkOverlap);
        documentParseService.setChunkingStrategy(chunkingStrategy);

        // 2. 初始化增强链
        List<TextEnhancer> enhancers = buildEnhancers();
        this.enhancerChain = new TextEnhancerChain(enhancers);

        log.info("🚀 [RAG向量化] enhancers={}, chunk-strategy={}, signature-cache={}, "
                        + "neighbor-expansion={}(window={}), parent-child={}",
                enhancers.isEmpty() ? "无" : enhancersConfig,
                chunkStrategyName, signatureCacheEnabled,
                neighborExpansionEnabled, neighborWindow, parentChildEnabled);
    }

    /**
     * 根据配置构建增强器列表。
     */
    private List<TextEnhancer> buildEnhancers() {
        List<TextEnhancer> list = new ArrayList<>();

        if (enhancersConfig == null || enhancersConfig.isBlank()) {
            return list;
        }

        for (String name : enhancersConfig.split(",")) {
            name = name.trim().toLowerCase();
            switch (name) {
                case "sliding-window" -> {
                    list.add(new SlidingWindowEnhancer());
                    log.info("  [增强器] 已注册: SlidingWindowEnhancer");
                }
                case "graph-entity" -> {
                    if (graphCrudService != null) {
                        // userId 运行时填充，GraphEntityEnhancer 从 contentRetriever 调用处获取
                        list.add(new GraphEntityEnhancer(graphCrudService, 0L));
                        log.info("  [增强器] 已注册: GraphEntityEnhancer");
                    } else {
                        log.warn("  [增强器] GraphCrudService 不可用，跳过 GraphEntityEnhancer");
                    }
                }
                default -> log.warn("  [增强器] 未知增强器: {}", name);
            }
        }

        return list;
    }

    /**
     * 获取指定会话的文档内容检索器
     */
    public ContentRetriever getContentRetriever(Long sessionId, List<Long> documentIds, Long userId) {
        if (documentIds == null || documentIds.isEmpty()) {
            return null;
        }

        Set<Long> docIdSet = new HashSet<>(documentIds);
        Set<Long> cachedIds = sessionDocumentIds.get(sessionId);

        if (cachedIds != null && cachedIds.equals(docIdSet)) {
            log.info("复用会话 {} 的 RAG 缓存，文档: {}", sessionId, docIdSet);

            return createStrictDocumentRetriever(
                    createFilteredVectorRetriever(userId, documentIds),
                    loadLocalDocumentContents(docIdSet, userId));
        }

        int newDocs = 0;
        for (Long docId : docIdSet) {
            if (!indexedDocuments.contains(docId)) {
                if (indexDocument(docId, userId)) {
                    indexedDocuments.add(docId);
                    newDocs++;
                }
            }
        }

        sessionDocumentIds.put(sessionId, docIdSet);
        log.info("已构建会话 {} 的 RAG 索引，文档: {}, 新索引: {} 个", sessionId, docIdSet, newDocs);

        return createStrictDocumentRetriever(
                createFilteredVectorRetriever(userId, documentIds),
                loadLocalDocumentContents(docIdSet, userId));
    }

    /**
     * 清除指定会话的 RAG 缓存
     */
    public void clearCache(Long sessionId) {
        sessionDocumentIds.remove(sessionId);
    }

    /**
     * 删除文档的所有向量嵌入
     */
    public void removeDocumentEmbeddings(Long documentId) {
        indexedDocuments.remove(documentId);
        documentSignatureCache.remove(documentId);
        log.info("已从缓存中移除文档索引: docId={}", documentId);
    }

    /**
     * 为当前用户重建所有文档的 ES 索引。
     * 先清除当前用户在 ES 中的所有旧数据，再重新索引全部文档。
     */
    public int reindexAll(Long userId) {
        // 1. 清除缓存中的索引记录
        indexedDocuments.clear();
        documentSignatureCache.clear();

        // 2. 删除 ES 中当前用户的所有旧文档数据
        int deleted = deleteUserEsDocuments(userId);
        log.info("已清除 ES 中用户 {} 的旧数据: {} 条", userId, deleted);

        // 3. 查询用户所有文档（未删除的），重新索引
        QueryWrapper qw = new QueryWrapper();
        qw.eq("user_id", userId);
        qw.eq("delete_flag", 0);
        List<Document> docs = documentService.list(qw);
        int successCount = 0;
        for (Document doc : docs) {
            try {
                triggerIndexing(doc.getId(), userId);
                successCount++;
                log.info("  [重建索引] 完成: docId={}, name={}", doc.getId(), doc.getName());
            } catch (Exception e) {
                log.error("  [重建索引] 失败: docId={}, error={}", doc.getId(), e.getMessage());
            }
        }
        log.info("✅ [重建索引] 用户 {} 完成: {}/{} 个文档重建成功", userId, successCount, docs.size());
        return successCount;
    }

    /**
     * 通过 ES deleteByQuery 删除指定用户在 ES 中的所有文档数据。
     */
    private int deleteUserEsDocuments(Long userId) {
        try {
            var response = esClient.deleteByQuery(d -> d
                    .index(esIndexName)
                    .query(q -> q
                            .term(t -> t
                                    .field("metadata.userId")
                                    .value(v -> v.stringValue(userId.toString()))
                            )
                    )
            );
            long deleted = response.deleted();
            log.info("  [ES清理] 用户 {}: {} 条文档已删除", userId, deleted);
            return (int) deleted;
        } catch (Exception e) {
            log.warn("  [ES清理] deleteByQuery 失败，尝试重建索引: {}", e.getMessage());
            // 如果删除失败（如索引不存在），尝试重建整个索引
            try {
                if (esClient.indices().exists(idx -> idx.index(esIndexName)).value()) {
                    esClient.indices().delete(d -> d.index(esIndexName));
                    log.info("  [ES清理] 删除索引 {}", esIndexName);
                }
            } catch (Exception ex) {
                log.warn("  [ES清理] 重建索引也失败: {}", ex.getMessage());
            }
            return 0;
        }
    }

    // ==================== 索引核心 ====================

    /**
     * 触发文档索引（上传后立即调用）。
     * 如果文档已索引且内容未变更则跳过，否则执行完整的向量化流程。
     */
    public void triggerIndexing(Long docId, Long userId) {
        log.info("🔨 [触发索引] 开始向量化文档: docId={}, userId={}", docId, userId);
        if (indexDocument(docId, userId)) {
            indexedDocuments.add(docId);
            log.info("✅ [触发索引] 向量化完成: docId={}", docId);
        } else {
            log.warn("⚠️ [触发索引] 向量化失败或跳过: docId={}", docId);
        }
    }

    private boolean indexDocument(Long docId, Long userId) {
        try {
            Document entity = documentService.getById(docId);
            if (entity == null || entity.getDeleteFlag() == 1) {
                log.warn("文档不存在或已删除: docId={}", docId);
                return false;
            }
            if (!entity.getUserId().equals(userId)) {
                log.warn("无权访问文档: docId={}, userId={}", docId, userId);
                return false;
            }

            Path filePath = Paths.get(uploadDir, entity.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn("文档文件不存在: {}", filePath);
                return false;
            }

            // 内容签名缓存：内容未变更则跳过重索引
            if (signatureCacheEnabled) {
                String newSig = computeSignature(filePath);
                String oldSig = documentSignatureCache.get(docId);
                if (newSig != null && newSig.equals(oldSig) && indexedDocuments.contains(docId)) {
                    log.info("🔒 [签名缓存] docId={} 内容未变更(SHA256={}), 跳过重索引", docId, newSig);
                    return true;
                }
                if (oldSig != null && newSig != null && !newSig.equals(oldSig)) {
                    log.info("📝 [签名缓存] docId={} 内容已变更: {} → {}, 重新索引", docId, oldSig, newSig);
                }
                documentSignatureCache.put(docId, newSig);
            }

            // 解析文档为 Chunk
            List<DocumentChunk> chunks = documentParseService.parse(filePath, entity.getFileType(), chunkingStrategy);

            // 应用增强链生成增强文本
            applyEnhancementChain(chunks, userId, entity.getFileType());

            // 构建元数据列表，并分离 "Embedding 用文本" 和 "存储用文本"
            List<ChunkEntry> entries = buildChunkEntries(entity, chunks, userId);

            if (entries.isEmpty()) {
                log.warn("文档未解析出可索引内容: docId={}", docId);
                return false;
            }

            // 手动 Embedding + 存储：增强文本只用于计算向量，原始文本存入 ES
            int indexed = 0;
            for (ChunkEntry entry : entries) {
                try {
                    // embedding 用增强文本（含 【上文】【下文】【实体关系】标记）
                    String textForEmbedding = entry.enhancedText != null
                            ? entry.enhancedText : entry.originalText;
                    Embedding embedding = embeddingModel.embed(textForEmbedding).content();

                    // 存储用原始文本（无标记，干净）
                    TextSegment segment = TextSegment.from(entry.originalText, entry.metadata);
                    embeddingStore.add(embedding, segment);
                    indexed++;
                } catch (Exception e) {
                    log.warn("  单个 chunk Embedding 失败: chunkIndex={}, error={}",
                            entry.chunkIndex, e.getMessage());
                }
            }
            log.info("文档已索引到 ES: docId={}, name={}, chunks={}, 增强文本用于向量={}",
                    docId, entity.getName(), indexed,
                    entries.stream().anyMatch(e -> e.enhancedText != null));

            // 如果是双层策略，生成细粒度索引
            if (ChunkingStrategyFactory.isDual(chunkStrategyName)) {
                indexDualFineGrained(entity, filePath, chunks, userId, docId);
            }

            return indexed > 0;
        } catch (Exception e) {
            log.error("索引文档失败: docId={}", docId, e);
            return false;
        }
    }

    /** 需要实体增强的文件类型（仅法律类文档启用 GraphEntityEnhancer） */
    private static final List<String> ENTITY_ENHANCED_TYPES = List.of("pdf", "docx", "xlsx", "xls");

    /**
     * 对每个 chunk 应用增强链，将增强文本设入 chunk.enhancedText。
     * 非法律类文档（txt/md 等）跳过 GraphEntityEnhancer。
     */
    private void applyEnhancementChain(List<DocumentChunk> chunks, Long userId, String fileType) {
        // GraphEntityEnhancer 需要运行时 userId，重新创建
        List<TextEnhancer> runtimeEnhancers = new ArrayList<>();
        boolean entityEnabled = fileType != null && ENTITY_ENHANCED_TYPES.contains(fileType.toLowerCase());
        for (String name : Optional.ofNullable(enhancersConfig).orElse("").split(",")) {
            name = name.trim().toLowerCase();
            if ("graph-entity".equals(name) && graphCrudService != null) {
                if (entityEnabled) {
                    runtimeEnhancers.add(new GraphEntityEnhancer(graphCrudService, userId));
                } else {
                    log.debug("  [增强器] fileType={}, 跳过 GraphEntityEnhancer", fileType);
                }
            } else if ("sliding-window".equals(name)) {
                runtimeEnhancers.add(new SlidingWindowEnhancer());
            }
        }
        TextEnhancerChain runtimeChain = new TextEnhancerChain(runtimeEnhancers);

        for (DocumentChunk chunk : chunks) {
            String enhanced = runtimeChain.enhance(chunk, chunks);
            if (!enhanced.equals(chunk.getContent())) {
                chunk.setEnhancedText(enhanced);
            }
        }

        int enhancedCount = (int) chunks.stream().filter(c -> c.getEnhancedText() != null).count();
        if (enhancedCount > 0) {
            log.info("  [增强链] 已增强 {} / {} 个 chunk", enhancedCount, chunks.size());
            // 输出首个增强样本便于直观验证
            chunks.stream().filter(c -> c.getEnhancedText() != null).findFirst().ifPresent(sample -> {
                log.info("  [增强样本] Chunk[{}] 原文前50字: {}",
                        sample.getChunkIndex(),
                        truncate(sample.getContent(), 50));
                log.info("  [增强样本] Chunk[{}] 增强后前100字: {}",
                        sample.getChunkIndex(),
                        truncate(sample.getEnhancedText(), 100));
            });
        }
    }

    /**
     * Embedding 与存储的中间载体。
     * enhancedText 仅用于计算向量，originalText 写入 ES 供检索返回。
     */
    private record ChunkEntry(
            String originalText,
            String enhancedText,
            int chunkIndex,
            Metadata metadata
    ) {}

    /**
     * 为每个 chunk 构建 {原始文本, 增强文本, 元数据} 三元组。
     * 增强文本只参与向量计算，不影响存储和检索展示。
     */
    private List<ChunkEntry> buildChunkEntries(
            Document entity, List<DocumentChunk> chunks, Long userId) {
        List<ChunkEntry> entries = new ArrayList<>();
        int enhancedCount = 0;
        for (DocumentChunk chunk : chunks) {
            if (chunk.getContent() == null || chunk.getContent().isBlank()) continue;
            if (chunk.getEnhancedText() != null) enhancedCount++;

            Metadata metadata = new Metadata()
                    .put("userId", userId.toString())
                    .put("documentId", entity.getId().toString())
                    .put("documentName", entity.getName())
                    .put("fileType", entity.getFileType() == null ? "" : entity.getFileType())
                    .put("pageNum", chunk.getPageNum() == null ? 0 : chunk.getPageNum())
                    .put("chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex())
                    .put("granularity", chunk.getGranularity() != null ? chunk.getGranularity() : "coarse");
            if (chunk.getParentChunkIndex() != null) {
                metadata.put("parentChunkIndex", chunk.getParentChunkIndex());
            }
            entries.add(new ChunkEntry(
                    chunk.getContent(),
                    chunk.getEnhancedText(),
                    chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 0,
                    metadata));
        }

        log.info("  [索引条目] 共 {} 个, 其中 {} 个使用增强文本计算向量 (granularity={})",
                entries.size(), enhancedCount,
                chunks.stream().findFirst().map(c -> c.getGranularity()).orElse("coarse"));

        return entries;
    }

    /**
     * 双层索引：用细粒度策略再次分块并索引。
     * Phase 4: 记录 fine→coarse 的父子映射（通过 charOffsetStart），
     * 在检索时若命中 fine chunk，将父 chunk 内容一并提供给 LLM。
     */
    private void indexDualFineGrained(Document entity, Path filePath,
                                       List<DocumentChunk> coarseChunks,
                                       Long userId, Long docId) {
        try {
            ChunkingStrategy fineStrategy = new FineChunkStrategy(
                    fineChunkSize, fineChunkOverlap);
            List<DocumentChunk> fineChunks = documentParseService.parse(
                    filePath, entity.getFileType(), fineStrategy);
            for (DocumentChunk chunk : fineChunks) {
                chunk.setGranularity("fine");
            }
            applyEnhancementChain(fineChunks, userId, entity.getFileType());

            // Phase 4: 建立 coarse→fine 父块偏移映射（用于 parentChunkIndex）
            List<int[]> coarseRanges = buildCoarseRanges(coarseChunks);

            List<ChunkEntry> entries = new ArrayList<>();
            for (DocumentChunk chunk : fineChunks) {
                if (chunk.getContent() == null || chunk.getContent().isBlank()) continue;

                Metadata metadata = new Metadata()
                        .put("userId", userId.toString())
                        .put("documentId", entity.getId().toString())
                        .put("documentName", entity.getName())
                        .put("fileType", entity.getFileType() == null ? "" : entity.getFileType())
                        .put("pageNum", chunk.getPageNum() == null ? 0 : chunk.getPageNum())
                        .put("chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex())
                        .put("granularity", "fine");

                // Phase 4: 计算父块索引
                if (parentChildEnabled && chunk.getCharOffsetStart() != null && !coarseRanges.isEmpty()) {
                    int parentIdx = findParentCoarseIndex(chunk.getCharOffsetStart(), coarseRanges);
                    if (parentIdx >= 0) {
                        metadata.put("parentChunkIndex", parentIdx);
                    }
                }

                String textForEmbedding = chunk.getEnhancedText() != null
                        ? chunk.getEnhancedText() : chunk.getContent();
                Embedding embedding = embeddingModel.embed(textForEmbedding).content();

                TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
                embeddingStore.add(embedding, segment);
                entries.add(new ChunkEntry(
                        chunk.getContent(), chunk.getEnhancedText(),
                        chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 0,
                        metadata));
            }

            int parentLinked = (int) entries.stream()
                    .filter(e -> e.metadata().getInteger("parentChunkIndex") != null).count();
            log.info("[双层索引] 细粒度索引完成: docId={}, chunks={}, parent-linked={}",
                    docId, entries.size(), parentLinked);
        } catch (Exception e) {
            log.warn("[双层索引] 细粒度索引失败，跳过: docId={}, error={}", docId, e.getMessage());
        }
    }

    /**
     * Phase 4: 从粗粒度 chunk 列表构建偏移范围列表。
     * 每个范围 = [charOffsetStart, charOffsetStart + chunkSize)，用于匹配细粒度 chunk 的归属。
     */
    private List<int[]> buildCoarseRanges(List<DocumentChunk> coarseChunks) {
        List<int[]> ranges = new ArrayList<>();
        for (DocumentChunk cc : coarseChunks) {
            if (cc.getCharOffsetStart() != null) {
                ranges.add(new int[]{cc.getCharOffsetStart(),
                        cc.getCharOffsetStart() + coarseChunkSize});
            }
        }
        return ranges;
    }

    /**
     * Phase 4: 在粗粒度偏移范围列表中查找包含指定偏移的父块索引。
     */
    private int findParentCoarseIndex(int offset, List<int[]> coarseRanges) {
        for (int i = 0; i < coarseRanges.size(); i++) {
            int[] range = coarseRanges.get(i);
            if (offset >= range[0] && offset < range[1]) {
                return i;
            }
        }
        // 如果没有精确匹配，找最近的（偏移不超过该范围的末尾）
        for (int i = 0; i < coarseRanges.size(); i++) {
            if (offset < coarseRanges.get(i)[1]) {
                return i;
            }
        }
        return -1;
    }

    private ContentRetriever createFilteredVectorRetriever(Long userId, List<Long> documentIds) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(buildDocumentFilter(userId, documentIds))
                .maxResults(esMaxResults)
                .minScore(esMinScore)
                .build();
    }

    private ContentRetriever createStrictDocumentRetriever(ContentRetriever vectorRetriever, List<Content> localContents) {
        return new ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                try {
                    List<Content> vectorContents = vectorRetriever.retrieve(query);
                    if (vectorContents != null && !vectorContents.isEmpty()) {
                        // Phase 3: 邻居块扩展 + Phase 4: 父块解析
                        List<Content> expanded = expandRetrievalResults(vectorContents);
                        log.info("[Document RAG] Vector retrieval: {} chunks → expanded to {} chunks",
                                vectorContents.size(), expanded.size());
                        return expanded;
                    }
                } catch (Exception e) {
                    log.warn("[Document RAG] Vector retrieval failed, falling back to parsed document chunks: {}", e.getMessage());
                }
                List<Content> fallbackContents = rankLocalContents(query, localContents);
                log.info("[Document RAG] Local parsed fallback returned {} chunks", fallbackContents.size());
                return fallbackContents;
            }
        };
    }

    /**
     * Phase 3 + Phase 4: 对向量检索结果进行后处理扩展。
     * <ul>
     *   <li>Phase 3: 每个命中的 chunk 的相邻块（chunkIndex ± window）也加入结果</li>
     *   <li>Phase 4: child chunk 命中时，将 parent chunk 加入结果</li>
     * </ul>
     * 相邻/父块通过 ES client 按 metadata 精确查询获取。
     *
     * 两遍扫描：第一遍处理邻居扩展，第二遍解析父块。
     * 分开以避免邻居扩展先占用了父块对应的 seenKeys 导致父块被跳过。
     */
    private List<Content> expandRetrievalResults(List<Content> results) {
        Set<String> seenKeys = new HashSet<>();
        List<Content> expanded = new ArrayList<>();
        int neighborSuccess = 0;
        int neighborTotal = 0;
        int parentSuccess = 0;
        int parentTotal = 0;

        // Pass 1: 加入原始结果 + 邻居扩展
        for (Content content : results) {
            TextSegment seg = content.textSegment();
            if (seg == null || seg.metadata() == null) {
                expanded.add(content);
                continue;
            }
            String docId = seg.metadata().getString("documentId");
            String userId = seg.metadata().getString("userId");
            Integer chunkIdx = seg.metadata().getInteger("chunkIndex");
            if (docId == null || userId == null || chunkIdx == null) {
                expanded.add(content);
                continue;
            }

            String key = docId + ":" + chunkIdx;
            if (!seenKeys.add(key)) continue;
            expanded.add(content);

            // Phase 3: 邻居块扩展
            if (neighborExpansionEnabled && neighborWindow > 0) {
                for (int offset = -neighborWindow; offset <= neighborWindow; offset++) {
                    if (offset == 0) continue;
                    int neighborIdx = chunkIdx + offset;
                    if (neighborIdx < 0) continue;
                    String nk = docId + ":" + neighborIdx;
                    if (seenKeys.add(nk)) {
                        neighborTotal++;
                        Content neighborContent = fetchNeighborFromEs(userId, docId, neighborIdx);
                        if (neighborContent != null) {
                            expanded.add(neighborContent);
                            neighborSuccess++;
                        }
                    }
                }
            }
        }

        // Pass 2: 父块解析（独立循环，避免与邻居扩展竞争 seenKeys）
        if (parentChildEnabled) {
            for (Content content : results) {
                TextSegment seg = content.textSegment();
                if (seg == null || seg.metadata() == null) continue;
                String docId = seg.metadata().getString("documentId");
                String userId = seg.metadata().getString("userId");
                Integer parentIdx = seg.metadata().getInteger("parentChunkIndex");
                if (docId == null || userId == null || parentIdx == null) continue;

                String pk = docId + ":" + parentIdx;
                if (seenKeys.add(pk)) {
                    parentTotal++;
                    Content parentContent = fetchNeighborFromEs(userId, docId, parentIdx);
                    if (parentContent != null) {
                        expanded.add(parentContent);
                        parentSuccess++;
                    }
                }
            }
        }

        log.info("  [检索扩展] ES查询: neighbor({}/{}成功) parent({}/{}成功) (total {} → {})",
                neighborSuccess, neighborTotal, parentSuccess, parentTotal,
                results.size(), expanded.size());
        if (neighborTotal > 0 && neighborSuccess == 0) {
            log.warn("  ⚠ [检索扩展] 邻居 ES 查询全部失败！请检查 ES 中 metadata 字段映射");
            diagnoseEsMapping();
        }
        if (parentTotal > 0 && parentSuccess == 0) {
            log.warn("  ⚠ [检索扩展] 父块 ES 查询全部失败！请检查 ES 中 metadata 字段映射");
            diagnoseEsMapping();
        }
        return expanded;
    }

    /**
     * 通过 ES 精确查询指定文档中特定 chunkIndex 的内容。
     * 用于 Phase 3 邻居扩展和 Phase 4 父块解析。
     * <p>
     * LangChain4j 将 metadata 存储在 ES 的 {@code metadata.*} 嵌套对象下，
     * 因此查询必须使用 {@code metadata.userId} 等带前缀的字段路径。
     */
    @SuppressWarnings("unchecked")
    private Content fetchNeighborFromEs(String userId, String documentId, int chunkIndex) {
        try {
            SearchResponse<Map<String, Object>> response = esClient.search(s -> s
                            .index(esIndexName)
                            .query(q -> q
                                    .bool(b -> {
                                        b.filter(f -> f.term(
                                                t -> t.field("metadata.userId").value(v -> v.stringValue(userId))));
                                        b.filter(f -> f.term(
                                                t -> t.field("metadata.documentId").value(v -> v.stringValue(documentId))));
                                        b.filter(f -> f.term(
                                                t -> t.field("metadata.chunkIndex").value(v -> v.longValue(chunkIndex))));
                                        return b;
                                    })
                            )
                            .size(1),
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return response.hits().hits().stream()
                    .findFirst()
                    .map(hit -> {
                        Map<String, Object> source = hit.source();
                        String text = source != null ? (String) source.get("text") : null;
                        return text != null && !text.isBlank() ? Content.from(text) : null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("  [ES精确查询] userId={}, docId={}, chunkIdx={} 查询失败: {}",
                    userId, documentId, chunkIndex, e.getMessage() != null
                            ? e.getMessage().substring(0, Math.min(120, e.getMessage().length()))
                            : "未知错误");
            return null;
        }
    }

    /**
     * 诊断 ES mapping：查询一条文档并输出 source 字段结构，用于排查 metadata 字段路径。
     */
    @SuppressWarnings("unchecked")
    private void diagnoseEsMapping() {
        try {
            // 查询任意一条文档
            var response = esClient.search(s -> s
                            .index(esIndexName)
                            .query(q -> q.matchAll(m -> m))
                            .size(1),
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            response.hits().hits().stream().findFirst().ifPresent(hit -> {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    log.info("  🔍 [ES诊断] 文档字段: {}", source.keySet());
                    // 如果包含 metadata 嵌套对象，也输出其子字段
                    Object meta = source.get("metadata");
                    if (meta instanceof Map) {
                        log.info("  🔍 [ES诊断] metadata子字段: {}",
                                ((Map<String, Object>) meta).keySet());
                    }
                }
            });
            // 输出索引 mapping
            var mapping = esClient.indices().getMapping(m -> m.index(esIndexName));
            log.info("  🔍 [ES诊断] mapping: {}", mapping.get(esIndexName).mappings());
        } catch (Exception e) {
            log.warn("  [ES诊断] 查询失败: {}", e.getMessage());
        }
    }

    private List<Content> loadLocalDocumentContents(Set<Long> docIds, Long userId) {
        List<Content> contents = new ArrayList<>();
        for (Long docId : docIds) {
            try {
                Document entity = documentService.getById(docId);
                if (entity == null || entity.getDeleteFlag() == 1 || !entity.getUserId().equals(userId)) {
                    continue;
                }
                Path filePath = Paths.get(uploadDir, entity.getFilePath());
                if (!Files.exists(filePath)) {
                    continue;
                }
                List<DocumentChunk> chunks = documentParseService.parse(filePath, entity.getFileType(), this.chunkingStrategy);
                for (DocumentChunk chunk : chunks) {
                    if (chunk.getContent() == null || chunk.getContent().isBlank()) {
                        continue;
                    }
                    int pageNum = chunk.getPageNum() == null ? 0 : chunk.getPageNum();
                    String text = "【文档：" + entity.getName() + "】\n"
                            + "【页码：" + pageNum + "】\n"
                            + chunk.getContent();
                    Metadata metadata = new Metadata()
                            .put("userId", userId.toString())
                            .put("documentId", entity.getId().toString())
                            .put("documentName", entity.getName())
                            .put("fileType", entity.getFileType() == null ? "" : entity.getFileType())
                            .put("pageNum", pageNum)
                            .put("chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex());
                    contents.add(Content.from(TextSegment.from(text, metadata)));
                }
            } catch (Exception e) {
                log.warn("[Document RAG] Failed to load local document chunks: docId={}, error={}", docId, e.getMessage());
            }
        }
        return contents;
    }

    private List<Content> rankLocalContents(Query query, List<Content> localContents) {
        if (localContents == null || localContents.isEmpty()) {
            return List.of();
        }
        String queryText = query == null ? "" : query.text();
        List<Content> ranked = new ArrayList<>(localContents);
        ranked.sort(Comparator.comparingInt((Content content) ->
                scoreContent(queryText, content.textSegment().text())).reversed());
        return ranked.stream()
                .limit(LOCAL_FALLBACK_MAX_RESULTS)
                .toList();
    }

    private int scoreContent(String query, String text) {
        if (query == null || query.isBlank() || text == null || text.isBlank()) {
            return 0;
        }
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int score = 0;
        for (String term : lowerQuery.split("[\\s,，。；;：:！!？?、]+")) {
            if (term.length() >= 2 && lowerText.contains(term)) {
                score += term.length() * 3;
            }
        }
        if (score > 0) {
            return score;
        }
        for (int i = 0; i < lowerQuery.length(); i++) {
            char c = lowerQuery.charAt(i);
            if (!Character.isWhitespace(c) && lowerText.indexOf(c) >= 0) {
                score++;
            }
        }
        return score;
    }

    private Filter buildDocumentFilter(Long userId, List<Long> documentIds) {
        Filter userFilter = MetadataFilterBuilder.metadataKey("userId").isEqualTo(userId.toString());
        if (documentIds == null || documentIds.isEmpty()) {
            return userFilter;
        }
        List<String> ids = documentIds.stream()
                .map(String::valueOf)
                .toList();
        Filter documentFilter = MetadataFilterBuilder.metadataKey("documentId").isIn(ids);
        return userFilter.and(documentFilter);
    }

    // ==================== 内容签名缓存 ====================

    /**
     * 计算文件内容签名（SHA256）。
     * 对小于 8KB 的文件全量 hash；大文件分段采样（头 + 1/3 处 + 2/3 处 + 尾各 4KB），
     * 避免仅取头部导致正文变更但签名不变的问题。
     */
    private String computeSignature(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] allBytes = Files.readAllBytes(filePath);
            int totalLen = allBytes.length;

            if (totalLen <= 8192) {
                md.update(allBytes);
            } else {
                int chunkSize = 4096;
                md.update(allBytes, 0, chunkSize);                                   // 头
                md.update(allBytes, totalLen / 3, Math.min(chunkSize, totalLen / 3)); // 1/3 处
                md.update(allBytes, totalLen * 2 / 3, Math.min(chunkSize, totalLen / 3)); // 2/3 处
                md.update(allBytes, totalLen - chunkSize, chunkSize);                // 尾
                md.update(longToBytes(totalLen));
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            md.update(longToBytes(attrs.lastModifiedTime().toMillis()));
            return HexFormat.of().formatHex(md.digest()).substring(0, 32);
        } catch (Exception e) {
            log.warn("[签名] 计算失败: {}", e.getMessage());
            return null;
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >> 56),
                (byte) (value >> 48),
                (byte) (value >> 40),
                (byte) (value >> 32),
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }
}
