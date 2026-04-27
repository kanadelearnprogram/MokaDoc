package com.kanade.backend.dto.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocumentUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
}
