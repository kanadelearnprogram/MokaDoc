package com.kanade.backend.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiChatService {


    @SystemMessage("You are a polite assistant")
    Flux<String> chat(String userMessage);

}
