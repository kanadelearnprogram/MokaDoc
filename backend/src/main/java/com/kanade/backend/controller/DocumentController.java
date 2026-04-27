package com.kanade.backend.controller;

import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.dto.document.DocumentUpdateRequest;
import com.kanade.backend.dto.document.DocumentVO;
import com.kanade.backend.entity.User;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/document")
@Tag(name = "文档管理", description = "提供文档上传、查询、更新、删除等功能")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @Resource
    private HttpServletRequest request;

    /**
     * 上传文档
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传文件，自动MD5去重")
    public BaseResponse<DocumentVO> upload(@RequestParam("file") MultipartFile file) {
        log.info("📤 [文档上传] 开始处理文件上传请求");
        
        Long userId = getLoginUserId();
        log.debug("👤 [文档上传] 用户ID: {}", userId);
        
        DocumentVO vo = documentService.upload(file, userId);
        
        log.info("✅ [文档上传] 文件上传成功 - docId={}, fileName={}", vo.getId(), vo.getName());
        return ResultUtils.success(vo);
    }

    /**
     * 检查MD5是否已存在（前端预查）
     */
    @GetMapping("/check-md5")
    @Operation(summary = "MD5查重", description = "检查当前用户是否已上传过相同MD5的文件")
    public BaseResponse<Boolean> checkMd5(@RequestParam String md5) {
        Long userId = getLoginUserId();
        boolean exists = documentService.getByMd5(userId, md5) != null;
        return ResultUtils.success(exists);
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据ID获取文档信息")
    public BaseResponse<DocumentVO> getById(@PathVariable Long id) {
        Long userId = getLoginUserId();
        DocumentVO vo = documentService.getDocumentVO(id, userId);
        return ResultUtils.success(vo);
    }

    /**
     * 获取当前用户所有文档
     */
    @GetMapping("/list")
    @Operation(summary = "文档列表", description = "获取当前用户的所有文档")
    public BaseResponse<List<DocumentVO>> list() {
        Long userId = getLoginUserId();
        List<DocumentVO> list = documentService.listByUser(userId);
        return ResultUtils.success(list);
    }

    /**
     * 更新文档信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新文档", description = "更新文档名称和描述")
    public BaseResponse<DocumentVO> update(@PathVariable Long id,
                                           @RequestBody DocumentUpdateRequest updateRequest) {
        Long userId = getLoginUserId();
        updateRequest.setId(id);
        DocumentVO vo = documentService.update(updateRequest, userId);
        return ResultUtils.success(vo);
    }

    /**
     * 删除文档（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "逻辑删除文档")
    public BaseResponse<Boolean> delete(@PathVariable Long id) {
        Long userId = getLoginUserId();
        documentService.delete(id, userId);
        return ResultUtils.success(true);
    }

    /**
     * 从Session获取当前登录用户ID
     */
    private Long getLoginUserId() {
        Object userObj = request.getSession().getAttribute("USER_LOGIN_STATE");
        if (userObj instanceof User currentUser) {
            return currentUser.getId();
        }
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
}
