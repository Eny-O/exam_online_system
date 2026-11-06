package com.ai.exam_auth_user.handler;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static BindingResult bindingResultWithFieldError() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "用户名不能为空"));
        return bindingResult;
    }

    private static BindingResult emptyBindingResult() {
        return new BeanPropertyBindingResult(new Object(), "target");
    }

    private void dummy(String value) {
    }

    private MethodArgumentNotValidException methodArgumentNotValidException(
            BindingResult bindingResult) throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @Test
    @DisplayName("业务异常返回异常自带的错误码与消息")
    void handleBusinessException() {
        ResponseResult<Void> result = handler.handleBusinessException(
                new BusinessException(ErrorCodeEnum.CLASS_NOT_FOUND));
        assertEquals("6001", result.getCode());
        assertEquals("班级不存在", result.getMessage());
    }

    @Test
    @DisplayName("权限不足返回 403")
    void handleAccessDeniedException() {
        ResponseResult<Void> result = handler.handleAccessDeniedException(
                new AccessDeniedException("denied"));
        assertEquals(ErrorCodeEnum.FORBIDDEN.getCode(), result.getCode());
        assertEquals(ErrorCodeEnum.FORBIDDEN.getMessage(), result.getMessage());
    }

    @Test
    @DisplayName("认证失败返回 401")
    void handleAuthenticationException() {
        ResponseResult<Void> result = handler.handleAuthenticationException(
                new BadCredentialsException("bad credentials"));
        assertEquals(ErrorCodeEnum.UNAUTHORIZED.getCode(), result.getCode());
        assertEquals(ErrorCodeEnum.UNAUTHORIZED.getMessage(), result.getMessage());
    }

    @Test
    @DisplayName("请求体校验失败返回首个字段错误消息")
    void handleMethodArgumentNotValidException_fieldError() throws Exception {
        ResponseResult<?> result = handler.handleMethodArgumentNotValidException(
                methodArgumentNotValidException(bindingResultWithFieldError()));
        assertEquals(ErrorCodeEnum.OPERATION_FAILED.getCode(), result.getCode());
        assertEquals("用户名不能为空", result.getMessage());
    }

    @Test
    @DisplayName("请求体校验无字段错误时返回默认消息")
    void handleMethodArgumentNotValidException_noFieldError() throws Exception {
        ResponseResult<?> result = handler.handleMethodArgumentNotValidException(
                methodArgumentNotValidException(emptyBindingResult()));
        assertEquals(ErrorCodeEnum.OPERATION_FAILED.getCode(), result.getCode());
        assertEquals("参数校验失败", result.getMessage());
    }

    @Test
    @DisplayName("参数绑定校验失败返回首个字段错误消息")
    void handleBindException_fieldError() {
        ResponseResult<?> result = handler.handleBindException(
                new BindException(bindingResultWithFieldError()));
        assertEquals(ErrorCodeEnum.OPERATION_FAILED.getCode(), result.getCode());
        assertEquals("用户名不能为空", result.getMessage());
    }

    @Test
    @DisplayName("参数绑定无字段错误时返回默认消息")
    void handleBindException_noFieldError() {
        ResponseResult<?> result = handler.handleBindException(
                new BindException(emptyBindingResult()));
        assertEquals(ErrorCodeEnum.OPERATION_FAILED.getCode(), result.getCode());
        assertEquals("参数绑定失败", result.getMessage());
    }

    @Test
    @DisplayName("未捕获异常返回 500")
    void handleException() {
        ResponseResult<Void> result = handler.handleException(new RuntimeException("boom"));
        assertEquals(ErrorCodeEnum.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals(ErrorCodeEnum.INTERNAL_ERROR.getMessage(), result.getMessage());
    }
}
