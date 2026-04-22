package com.kanade.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.SessionDocument;
import com.kanade.backend.mapper.SessionDocumentMapper;
import com.kanade.backend.service.SessionDocumentService;
import org.springframework.stereotype.Service;

/**
 * 会话文档关联表 服务层实现。
 *
 * @author kanade
 */
@Service
public class SessionDocumentServiceImpl extends ServiceImpl<SessionDocumentMapper, SessionDocument>  implements SessionDocumentService {

}
