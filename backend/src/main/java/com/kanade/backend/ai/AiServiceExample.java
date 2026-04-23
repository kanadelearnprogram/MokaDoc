package com.kanade.backend.ai;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI服务使用示例
 * 演示如何使用AiServiceFactory获取不同类型的AI服务
 */
@Slf4j
@Service
public class AiServiceExample {
    
    @Resource
    private AiServiceFactory aiServiceFactory;
    
    /**
     * 示例1: 使用通用聊天助手
     */
    public Flux<String> chatExample(String userMessage) {
        // 方式1: 通过枚举类型获取
        AiChatService chatService = aiServiceFactory.getService(AiModelType.CHAT_ASSISTANT);
        return chatService.chat(userMessage);
    }
    
    /**
     * 示例2: 使用文档分析专家
     */
    public Flux<String> analyzeDocument(String documentContent) {
        // 方式2: 使用便捷方法
        DocumentAnalystService analystService = aiServiceFactory.getDocumentAnalyst();
        return analystService.analyze(documentContent);
    }
    

    /**
     * 示例5: 动态根据类型选择AI服务
     */
    public Flux<String> dynamicServiceSelection(String modelTypeCode, String userInput) {
        // 根据code获取枚举
        AiModelType modelType = AiModelType.getByCode(modelTypeCode);
        
        if (modelType == null) {
            log.warn("不支持的模型类型: {}", modelTypeCode);
            // 默认使用通用聊天助手
            modelType = AiModelType.CHAT_ASSISTANT;
        }
        
        // 根据类型调用不同的服务
        return switch (modelType) {
            case CHAT_ASSISTANT -> 
                aiServiceFactory.getChatAssistant().chat(userInput);
            
            case DOCUMENT_ANALYST -> 
                aiServiceFactory.getDocumentAnalyst().analyze(userInput);
            
//            case CODE_ASSISTANT ->
//                aiServiceFactory.getCodeAssistant().explainCode(userInput);
//
//            case TRANSLATION_ASSISTANT ->
//                // 翻译需要额外参数，这里简化处理
//                aiServiceFactory.getTranslationAssistant().translate(userInput, "英文");
            default -> throw new RuntimeException("fuck");
        };
    }
}
