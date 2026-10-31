package com.ai.exam_ai.handler;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获并封装各类异常为 ResponseResult 响应，避免异常堆栈直接暴露给前端。
 * 风格与 exam-core 的 GlobalExceptionHandler 保持一致，仅保留 AI 模块实际触发的异常类型。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理，使用异常中携带的具体错误码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ResponseResult.error(e.getErrorCode(), e.getMessage());
    }

    /**
     * 处理 JSR-303 参数校验异常 (@RequestBody 触发)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验失败: {}", message);
        return ResponseResult.error(message);
    }

    /**
     * 处理 JSR-303 参数校验异常 (@RequestParam / @PathVariable 触发)
     */
    @ExceptionHandler(BindException.class)
    public ResponseResult<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数绑定失败";
        log.warn("参数绑定失败: {}", message);
        return ResponseResult.error(message);
    }

    /**
     * 未捕获的服务器异常兜底处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        log.error("服务器异常: {}", e.getMessage());
        return ResponseResult.error(ErrorCodeEnum.INTERNAL_ERROR);
    }
}
