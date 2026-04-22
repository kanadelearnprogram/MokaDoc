package com.kanade.backend.service;

import com.kanade.backend.entity.Document;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档服务层测试类
 * 测试SysDocumentService的基本增删改查功能，验证数据库连接
 */
@SpringBootTest
@Transactional
@Rollback
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentServiceTest {

    @Autowired
    private DocumentService sysDocumentService;

    private static Long testDocumentId;
    private static final Long TEST_USER_ID = 1L;

    /**
     * 测试新增文档
     */
    @Test
    @Order(1)
    @DisplayName("测试新增文档")
    void testSave() {
        Document document = new Document();
        document.setUserId(TEST_USER_ID);
        document.setName("测试文档.pdf");
        document.setFileType("pdf");
        document.setFileSize(1024000L);
        document.setFilePath("/uploads/documents/test_doc.pdf");
        document.setFileMd5("d41d8cd98f00b204e9800998ecf8427e");
        document.setDescription("这是一个测试文档");
        document.setUploadTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setDeleteFlag(0);

        boolean result = sysDocumentService.save(document);
        assertTrue(result, "文档保存应该成功");
        assertNotNull(document.getId(), "保存后文档ID不应为空");
        
        testDocumentId = document.getId();
        System.out.println("创建的文档ID: " + testDocumentId);
    }

    /**
     * 测试根据ID查询文档
     */
    @Test
    @Order(2)
    @DisplayName("测试根据ID查询文档")
    void testGetById() {
        assertNotNull(testDocumentId, "测试文档ID不应为空");
        
        Document document = sysDocumentService.getById(testDocumentId);
        assertNotNull(document, "查询结果不应为空");
        assertEquals("测试文档.pdf", document.getName(), "文档名称应该匹配");
        assertEquals("pdf", document.getFileType(), "文件类型应该匹配");
        
        System.out.println("查询到的文档: " + document.getName());
    }

    /**
     * 测试查询所有文档
     */
    @Test
    @Order(3)
    @DisplayName("测试查询所有文档")
    void testList() {
        List<Document> documentList = sysDocumentService.list();
        assertNotNull(documentList, "文档列表不应为空");
        
        System.out.println("文档总数: " + documentList.size());
    }

    /**
     * 测试更新文档
     */
    @Test
    @Order(4)
    @DisplayName("测试更新文档")
    void testUpdate() {
        assertNotNull(testDocumentId, "测试文档ID不应为空");
        
        Document document = sysDocumentService.getById(testDocumentId);
        assertNotNull(document, "文档应该存在");
        
        document.setName("更新后的文档.pdf");
        document.setDescription("这是更新后的文档描述");
        document.setUpdateTime(LocalDateTime.now());
        
        boolean result = sysDocumentService.updateById(document);
        assertTrue(result, "文档更新应该成功");
        
        Document updatedDocument = sysDocumentService.getById(testDocumentId);
        assertEquals("更新后的文档.pdf", updatedDocument.getName(), "文档名称应该已更新");
        
        System.out.println("文档已更新");
    }

    /**
     * 测试删除文档
     */
    @Test
    @Order(5)
    @DisplayName("测试删除文档")
    void testRemove() {
        assertNotNull(testDocumentId, "测试文档ID不应为空");
        
        boolean result = sysDocumentService.removeById(testDocumentId);
        assertTrue(result, "文档删除应该成功");
        
        Document deletedDocument = sysDocumentService.getById(testDocumentId);
        assertNull(deletedDocument, "删除后查询应该返回null（逻辑删除）");
        
        System.out.println("文档已删除");
    }

    /**
     * 测试统计文档数量
     */
    @Test
    @Order(6)
    @DisplayName("测试统计文档数量")
    void testCount() {
        long count = sysDocumentService.count();
        assertTrue(count >= 0, "文档数量应该大于等于0");
        System.out.println("当前文档总数: " + count);
    }
}
