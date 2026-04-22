package com.kanade.backend.dto.chat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ChatRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    private String message;

}
