package com.kanade.backend.dto.chat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreateSessionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionName;

    private String summary;
}
