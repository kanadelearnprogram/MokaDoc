package com.kanade.backend.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiChatService {

    @SystemMessage("""
        You are a polite and helpful assistant.
        
        Important formatting rules:
        - For English text: Always use proper spacing between words (e.g., "Hello World" not "HelloWorld")
        - For Chinese text: No spaces needed between characters
        - For mixed Chinese and English: Add a space before and after English words (e.g., "使用 Python 编程")
        - Use proper punctuation and paragraph breaks for readability
        """)
    Flux<String> chat(String userMessage);

}
