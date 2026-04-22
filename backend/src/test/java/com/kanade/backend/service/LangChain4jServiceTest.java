package com.kanade.backend.service;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * LangChain4j 流式聊天测试（使用Spring注入）
 */
@Slf4j
@SpringBootTest
public class LangChain4jServiceTest {

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * AI 助手接口
     */
    interface Assistant {
        TokenStream chat(String message);
    }

    @Test
    public void testStreamingChatWithSpringInjection() throws Exception {
        // 使用注入的模型创建 AI 服务
        Assistant assistant = AiServices.create(Assistant.class, streamingChatModel);

        System.out.println("=== 开始流式聊天 ===\n");

        TokenStream tokenStream = assistant.chat("什么是微服务架构？请用简单的语言解释");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 设置回调
        tokenStream.onPartialResponse(partialResponse -> {
            // 流式输出，逐字打印
            System.out.print(partialResponse);
        })
        .onCompleteResponse(response -> {
            futureResponse.complete(response);
            System.out.println("\n\n=== 聊天完成 ===");
            log.info("Token使用情况: {}", response.tokenUsage());
        })
        .onError(error -> {
            futureResponse.completeExceptionally(error);
            log.error("聊天出错: {}", error.getMessage(), error);
        })
        .start();

        // 等待响应完成（最多30秒）
        ChatResponse chatResponse = futureResponse.get(30, SECONDS);
        
        log.info("测试完成！");
    }

    @Test
    public void testMultipleQuestions() throws Exception {
        Assistant assistant = AiServices.create(Assistant.class, streamingChatModel);

        String[] questions = {
            "Java和Python有什么区别？",
            "什么是RESTful API？",
            "如何优化数据库查询性能？"
        };

        for (int i = 0; i < questions.length; i++) {
            System.out.println("\n========== 问题 " + (i + 1) + " ==========");
            System.out.println("问: " + questions[i]);
            System.out.print("答: ");

            CompletableFuture<Void> future = new CompletableFuture<>();

            assistant.chat(questions[i])
                    .onPartialResponse(response -> System.out.print(response))
                    .onCompleteResponse(response -> {
                        System.out.println();
                        future.complete(null);
                    })
                    .onError(future::completeExceptionally)
                    .start();

            future.get(30, SECONDS);
            
            // 避免请求过快
            Thread.sleep(1000);
        }
    }
}
