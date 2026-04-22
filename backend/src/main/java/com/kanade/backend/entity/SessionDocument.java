package com.kanade.backend.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话文档关联表 实体类。
 *
 * @author kanade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_session_document")
public class SessionDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 关联时间
     */
    private LocalDateTime createTime;

}
