package com.ai.exam_common.common.security;

import com.ai.exam_common.common.constant.JwtConstant;
import com.ai.exam_common.common.utils.JwtUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器（跨服务共享）
 * <p>
 * 从 Header 提取 JWT Token 并验证，通过 {@link LoginUserReader} 获取登录主体，
 * 不关心登录态存放在哪（Redis / 远程 auth-user）。
 * Token 无效或缺失时直接放行（不返回 401），由宿主服务注册的 AuthenticationEntryPoint 统一处理。
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final LoginUserReader loginUserReader;

    private final Cache<String, LoginUserPrincipal> caffeineCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(5000)
            .build();

    public JwtAuthenticationTokenFilter(LoginUserReader loginUserReader) {
        this.loginUserReader = loginUserReader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头提取 Token
        String token = request.getHeader(JwtConstant.TOKEN_HEADER);

        // Token 缺失时直接放行，不在此处返回 401，
        // 由宿主服务 SecurityConfig 中注册的 AuthenticationEntryPoint 统一处理未认证响应
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token 存在但校验失败（过期或签名错误），同样放行交由 AuthenticationEntryPoint 处理
        // Token 合法，从中解析出用户 ID
        String userId = JwtUtil.getSubject(token);
        if (!StringUtils.hasText(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅在 SecurityContext 中尚无认证信息时才填充，避免重复设置
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 根据 userId 读取登录时缓存的登录主体（含用户信息和权限列表）
            LoginUserPrincipal principal = caffeineCache.get(userId, k -> loginUserReader.read(userId));
            if (principal != null) {
                // 构建认证令牌并写入 SecurityContext，后续 Controller 可通过 SecurityContextHolder 获取当前用户
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
