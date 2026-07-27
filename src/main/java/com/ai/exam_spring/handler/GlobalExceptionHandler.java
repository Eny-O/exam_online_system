package com.ai.exam_spring.handler;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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
        return ResponseResult.error(e.getErrorCode());
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
     * 未捕获的服务器异常兜底处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        log.error("服务器异常: {}", e.getMessage());
        return ResponseResult.error(ErrorCodeEnum.INTERNAL_ERROR);
    }
}
