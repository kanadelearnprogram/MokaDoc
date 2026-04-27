package com.kanade.backend.service;

import com.mybatisflex.core.service.IService;
import com.kanade.backend.dto.document.DocumentUpdateRequest;
import com.kanade.backend.dto.document.DocumentVO;
import com.kanade.backend.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档表 服务层。
 *
 * @author kanade
 */
public interface DocumentService extends IService<Document> {

    /**
     * 按用户和MD5查询文档（用于查重）
     */
    Document getByMd5(Long userId, String md5);

    /**
     * 上传文件
     */
    DocumentVO upload(MultipartFile file, Long userId);

    /**
     * 查询单个文档（校验归属）
     */
    DocumentVO getDocumentVO(Long id, Long userId);

    /**
     * 查询用户所有文档
     */
    List<DocumentVO> listByUser(Long userId);

    /**
     * 更新文档信息
     */
    DocumentVO update(DocumentUpdateRequest req, Long userId);

    /**
     * 逻辑删除文档
     */
    boolean delete(Long id, Long userId);
}
