package com.kanade.backend.dto.chat;

import com.kanade.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatQueryRequest extends PageRequest implements Serializable {
    Long userId;

    Long SessionId;
    private LocalDateTime lastCreateTime;
}
