package com.ai.exam_spring.handler;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获并封装各类异常为 ResponseResult 响应，避免异常堆栈直接暴露给前端。
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
        return ResponseResult.error(e.getErrorCode(),e.getMessage());
    }

    /**
     * 非法访问异常处理
     */
    @ExceptionHandler(IllegalAccessException.class)
    public ResponseResult<Void> handleIllegalAccessException(IllegalAccessException e) {
        log.error("非法访问: {}", e.getMessage());
        return ResponseResult.error(ErrorCodeEnum.BAD_REQUEST);
    }

    /**
     * 认证失败异常处理（用户名或密码错误）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseResult<Void> handleBadCredentials(BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ResponseResult.error(ErrorCodeEnum.UNAUTHORIZED);
    }

    /**
     * 处理 JSR-303 参数校验异常 (@RequestBody 触发)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取校验失败的字段和错误信息
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
        return ResponseResult.error( message);
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
