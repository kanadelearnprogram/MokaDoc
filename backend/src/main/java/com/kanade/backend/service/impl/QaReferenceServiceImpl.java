package com.kanade.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.QaReference;
import com.kanade.backend.mapper.QaReferenceMapper;
import com.kanade.backend.service.QaReferenceService;
import org.springframework.stereotype.Service;

/**
 * 回答引用表 服务层实现。
 *
 * @author kanade
 */
@Service
public class QaReferenceServiceImpl extends ServiceImpl<QaReferenceMapper, QaReference>  implements QaReferenceService {

}
