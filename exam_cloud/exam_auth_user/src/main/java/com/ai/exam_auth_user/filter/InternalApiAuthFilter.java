package com.ai.exam_auth_user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部接口调用方鉴权过滤器。
 * <p>
 * 仅对 {@code /inner/**} 路径生效：校验请求头 {@code X-Internal-Token} 是否等于配置的内部密钥。
 * 缺失或不符一律返回 401，其余路径完全放行，不影响现有 JWT 认证链路。
 * <p>
 * 与 {@code SecurityConfig} 配合：{@code /inner/**} 仍登记为 {@code permitAll()}（让请求穿过认证门槛），
 * 真正的鉴权由本过滤器完成，避免未授权调用方直连 8082 即可获取学生/班级批量数据。
 */
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${exam.internal-api-key}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只对 /inner/ 开头的路径生效；其它路径（JWT 保护的业务接口）完全不动
        return !request.getRequestURI().startsWith("/inner/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (!StringUtils.hasText(token) || !internalApiKey.equals(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"内部接口鉴权失败：缺少或非法的 X-Internal-Token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
