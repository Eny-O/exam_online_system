package com.ai.exam_auth_user.handler;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证处理器
 * <p>
 * 当未登录用户访问需要认证的接口时，由 Spring Security 调用此处理器返回 401 响应。
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ResponseResult<Void> result = ResponseResult.error(ErrorCodeEnum.UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
