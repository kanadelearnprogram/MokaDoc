package com.kanade.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.QaMessage;
import com.kanade.backend.mapper.QaMessageMapper;
import com.kanade.backend.service.QaMessageService;
import org.springframework.stereotype.Service;

/**
 * 会话消息表 服务层实现。
 *
 * @author kanade
 */
@Service
public class QaMessageServiceImpl extends ServiceImpl<QaMessageMapper, QaMessage>  implements QaMessageService {

}
