package com.kanade.backend.service.impl;

import com.kanade.backend.mapper.DocumentMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.kanade.backend.entity.Document;

import com.kanade.backend.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * 文档表 服务层实现。
 *
 * @author kanade
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document>  implements DocumentService {

}
