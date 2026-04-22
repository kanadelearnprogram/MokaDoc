package com.kanade.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.QaSession;
import com.kanade.backend.mapper.QaSessionMapper;
import com.kanade.backend.service.QaSessionService;
import org.springframework.stereotype.Service;

/**
 * 问答会话表 服务层实现。
 *
 * @author kanade
 */
@Service
public class QaSessionServiceImpl extends ServiceImpl<QaSessionMapper, QaSession>  implements QaSessionService {

}
