package com.ai.exam_spring.handler;

import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }


    // 参数校验异常
    @ExceptionHandler(IllegalAccessException.class)
    public ResponseResult<Void> handleIllegalAccessException(IllegalAccessException e) {
        log.error("参数异常: {}", e.getMessage());
        return ResponseResult.error("400","参数异常");
    }
    // 认证校验异常
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseResult<Void> handleBadCredentials(BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ResponseResult.error("401", "用户名或密码错误");
    }

    // 统一处理所有异常
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        log.error("服务器异常: {}", e.getMessage());
        return ResponseResult.error("服务器异常");
    }
}
