package com.kanade.backend.ai;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI服务工厂
 * 根据模型类型提供对应的AI服务实例
 */
@Slf4j
@Component
public class AiServiceFactory {
    
    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;
    
    /**
     * 缓存已创建的AI服务实例，避免重复创建
     */
    private final Map<AiModelType, Object> serviceCache = new ConcurrentHashMap<>();
    
    /**
     * 获取指定类型的AI服务
     *
     * @param modelType AI模型类型
     * @return 对应的AI服务实例
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(AiModelType modelType) {
        if (modelType == null) {
            throw new IllegalArgumentException("AI模型类型不能为空");
        }
        
        // 从缓存中获取
        return (T) serviceCache.computeIfAbsent(modelType, this::createService);
    }
    
    /**
     * 获取通用聊天助手服务
     *
     * @return AiChatService实例
     */
    public AiChatService getChatAssistant() {
        return getService(AiModelType.CHAT_ASSISTANT);
    }
    
    /**
     * 获取文档分析专家服务
     *
     * @return DocumentAnalystService实例
     */
    public DocumentAnalystService getDocumentAnalyst() {
        return getService(AiModelType.DOCUMENT_ANALYST);
    }
        

    
    /**
     * 根据模型类型创建对应的AI服务
     *
     * @param modelType AI模型类型
     * @return AI服务实例
     */
    private Object createService(AiModelType modelType) {
        log.info("创建AI服务: {}", modelType.getDescription());
        
        return switch (modelType) {
            case CHAT_ASSISTANT -> 
                AiServices.create(AiChatService.class, openAiStreamingChatModel);
            
            case DOCUMENT_ANALYST -> 
                AiServices.create(DocumentAnalystService.class, openAiStreamingChatModel);
            
//            case CODE_ASSISTANT ->
//                AiServices.create(CodeAssistantService.class, openAiStreamingChatModel);
//
//            case TRANSLATION_ASSISTANT ->
//                AiServices.create(TranslationAssistantService.class, openAiStreamingChatModel);
//
            default -> 
                throw new IllegalArgumentException("不支持的AI模型类型: " + modelType.getCode());
        };
    }
    
    /**
     * 清除缓存的服务实例（用于测试或重新加载）
     */
    public void clearCache() {
        serviceCache.clear();
        log.info("AI服务缓存已清除");
    }
}
