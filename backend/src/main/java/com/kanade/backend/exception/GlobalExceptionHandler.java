package com.kanade.backend.exception;


import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

/**
 * 全局异常处理器
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("❌ [业务异常] {}", e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public BaseResponse<?> multipartExceptionHandler(MultipartException e) {
        log.warn("⚠️ [文件上传] 文件格式错误 - {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件格式错误，请使用 multipart/form-data 格式上传文件");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("❌ [系统异常] {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
