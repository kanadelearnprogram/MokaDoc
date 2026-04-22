package com.kanade.backend.controller;

import com.kanade.backend.ai.AiChatService;
import com.kanade.backend.dto.chat.ChatRequest;
import com.kanade.backend.entity.User;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.exception.ThrowUtils;
import com.kanade.backend.service.UserService;
import com.kanade.backend.sse.SseEmitterManager;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/chat")
@Tag(name = "用户管理", description = "提供用户注册、登录、查询等功能")
public class ChatController {

    @Resource
    SseEmitterManager sseEmitterManager;

    @Resource
    UserService userService;

    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @GetMapping("/chat-stream")
    public Flux<String> chat(@RequestParam String prompt) {
        AiChatService aiChatService = AiServices.create(AiChatService.class, openAiStreamingChatModel);
        return aiChatService.chat(prompt);
    }
}
