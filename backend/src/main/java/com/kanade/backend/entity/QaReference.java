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
 * 回答引用表 实体类。
 *
 * @author kanade
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_qa_reference")
public class QaReference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 引用ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * AI消息ID
     */
    private Long messageId;

    /**
     * 引用文档ID
     */
    private Long documentId;

    /**
     * 引用原文
     */
    private String content;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
