package com.kanade.backend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kanade.backend.dto.chat.ChatQueryRequest;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.QaMessage;
import com.kanade.backend.mapper.QaMessageMapper;
import com.kanade.backend.service.QaMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会话消息表 服务层实现。
 *
 * @author kanade
 */
@Service
public class QaMessageServiceImpl extends ServiceImpl<QaMessageMapper, QaMessage>  implements QaMessageService {

    @Override
    public Page<QaMessage> listSessionChatByPage(Long sessionId, int pageSize, LocalDateTime lastCreateTime, Long id) {
        ChatQueryRequest chatQueryRequest = new ChatQueryRequest();
        chatQueryRequest.setUserId(id);
        chatQueryRequest.setSessionId(sessionId);
        chatQueryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getChatQueryWrapper(chatQueryRequest);
        return this.page(Page.of(1,pageSize),queryWrapper);
    }

    private QueryWrapper getChatQueryWrapper(ChatQueryRequest chatSessionQueryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatSessionQueryDTO == null) {
            return queryWrapper;
        }
        Long userId = chatSessionQueryDTO.getUserId();
        Long sessionId = chatSessionQueryDTO.getSessionId();
        LocalDateTime lastCreateTime = chatSessionQueryDTO.getLastCreateTime();
        String sortField = chatSessionQueryDTO.getSortField();
        String sortOrder = chatSessionQueryDTO.getSortOrder();
        // 拼接查询条件
        queryWrapper
//                .eq("user_id", userId)
                .eq("session_id",sessionId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("create_time", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("create_time", false);
        }
        return queryWrapper;
    }
}
