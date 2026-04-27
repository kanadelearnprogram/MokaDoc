package com.kanade.backend.ai.rag;

import com.kanade.backend.entity.Document;
import com.kanade.backend.service.DocumentService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档 RAG 服务 —— 基于 Easy RAG 加载文档并构建内容检索器
 */
@Slf4j
@Component
public class DocumentRagService {

    @Value("${file.upload.dir:./uploads/documents}")
    private String uploadDir;

    @Resource
    private DocumentService documentService;

    private final Map<Long, InMemoryEmbeddingStore<TextSegment>> sessionCache = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> sessionDocumentIds = new ConcurrentHashMap<>();

    /**
     * 获取指定会话的文档内容检索器
     *
     * @param sessionId   会话ID
     * @param documentIds 文档ID列表
     * @param userId      用户ID（用以权限校验）
     * @return ContentRetriever 或 null（如果没有可加载的文档）
     */
    public ContentRetriever getContentRetriever(Long sessionId, List<Long> documentIds, Long userId) {
        if (documentIds == null || documentIds.isEmpty()) {
            return null;
        }

        Set<Long> docIdSet = new HashSet<>(documentIds);
        Set<Long> cachedIds = sessionDocumentIds.get(sessionId);

        // 文档 ID 集合相同则复用缓存
        if (cachedIds != null && cachedIds.equals(docIdSet)) {
            InMemoryEmbeddingStore<TextSegment> cached = sessionCache.get(sessionId);
            if (cached != null) {
                log.info("复用会话 {} 的 RAG 缓存，文档: {}", sessionId, docIdSet);
                return EmbeddingStoreContentRetriever.from(cached);
            }
        }

        // 构建新的 embedding store
        InMemoryEmbeddingStore<TextSegment> embeddingStore = buildEmbeddingStore(documentIds, userId);
        if (embeddingStore == null) {
            return null;
        }

        sessionCache.put(sessionId, embeddingStore);
        sessionDocumentIds.put(sessionId, docIdSet);
        log.info("已构建会话 {} 的 RAG 索引，文档: {}", sessionId, docIdSet);

        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }

    /**
     * 清除指定会话的 RAG 缓存
     */
    public void clearCache(Long sessionId) {
        sessionCache.remove(sessionId);
        sessionDocumentIds.remove(sessionId);
    }

    private InMemoryEmbeddingStore<TextSegment> buildEmbeddingStore(List<Long> documentIds, Long userId) {
        List<dev.langchain4j.data.document.Document> documents = new ArrayList<>();

        for (Long docId : documentIds) {
            try {
                Document entity = documentService.getById(docId);
                if (entity == null || entity.getDeleteFlag() == 1) {
                    log.warn("文档不存在或已删除: docId={}", docId);
                    continue;
                }
                if (!entity.getUserId().equals(userId)) {
                    log.warn("无权访问文档: docId={}, userId={}", docId, userId);
                    continue;
                }

                Path filePath = Paths.get(uploadDir, entity.getFilePath());
                if (!Files.exists(filePath)) {
                    log.warn("文档文件不存在: {}", filePath);
                    continue;
                }

                dev.langchain4j.data.document.Document doc =
                        dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(filePath);
                documents.add(doc);
                log.info("已加载文档文件: docId={}, name={}", docId, entity.getName());
            } catch (Exception e) {
                log.error("加载文档失败: docId={}", docId, e);
            }
        }

        if (documents.isEmpty()) {
            return null;
        }

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        log.info("RAG 索引完成，共 {} 个文档", documents.size());
        return embeddingStore;
    }
}
