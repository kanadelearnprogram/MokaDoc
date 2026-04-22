package com.kanade.backend.service;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * LangChain4j 流式聊天测试
 */
@Slf4j
@SpringBootTest
public class StreamingChatTest {

    /**
     * AI 助手接口
     */
    interface Assistant {
        TokenStream chat(String message);
    }

    @Test
    public void testStreamingChat() throws Exception {
        // 创建流式聊天模型
        // 注意：请使用你自己的 OpenAI API Key
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey("your-openai-api-key") // 替换为你的 API Key
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建 AI 服务
        Assistant assistant = AiServices.create(Assistant.class, model);

        // 发起流式聊天请求
        TokenStream tokenStream = assistant.chat("讲一个程序员的笑话");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 设置回调
        tokenStream.onPartialResponse(partialResponse -> {
            // 逐字打印响应（流式输出）
            System.out.print(partialResponse);
        })
        .onCompleteResponse(response -> {
            // 完整响应完成
            futureResponse.complete(response);
            System.out.println("\n\n=== 完整响应 ===");
            System.out.println(response);
        })
        .onError(error -> {
            // 错误处理
            futureResponse.completeExceptionally(error);
            log.error("聊天出错: {}", error.getMessage(), error);
        })
        .start();

        // 等待响应完成（最多30秒）
        ChatResponse chatResponse = futureResponse.get(30, SECONDS);
        
        log.info("聊天完成，Token使用: {}", chatResponse.tokenUsage());
    }

    @Test
    public void testSimpleChat() throws Exception {
        // 简化版本的流式聊天测试
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey("your-openai-api-key") // 替换为你的 API Key
                .modelName(GPT_4_O_MINI)
                .build();

        Assistant assistant = AiServices.create(Assistant.class, model);

        System.out.println("=== 开始流式聊天 ===\n");
        
        TokenStream tokenStream = assistant.chat("用三句话介绍什么是人工智能");

        CompletableFuture<Void> future = new CompletableFuture<>();

        tokenStream.onPartialResponse(response -> System.out.print(response))
                .onCompleteResponse(response -> {
                    System.out.println("\n\n=== 聊天完成 ===");
                    future.complete(null);
                })
                .onError(future::completeExceptionally)
                .start();

        // 等待完成
        future.get(30, SECONDS);
    }
}
