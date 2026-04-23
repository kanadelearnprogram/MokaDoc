package com.kanade.backend.dto;

import com.kanade.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatSessionQueryDTO extends PageRequest implements Serializable {


    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String sessionName;

    private String summary;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 游标查询 - 最后一条记录的创建时间
     * 用于分页查询，获取早于此时间的记录
     */
    private LocalDateTime lastCreateTime;

    private static final long serialVersionUID = 1L;


}
