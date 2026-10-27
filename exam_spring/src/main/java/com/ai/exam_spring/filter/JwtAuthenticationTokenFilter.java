package com.ai.exam_spring.filter;

import com.ai.exam_spring.common.constant.JwtConstant;
import com.ai.exam_spring.common.constant.RedisConstant;
import com.ai.exam_spring.security.LoginUser;
import com.ai.exam_spring.utils.JwtUtil;
import com.ai.exam_spring.utils.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器
 * <p>
 * 在 Spring Security 过滤器链中拦截请求，从 Header 中提取 JWT Token 并验证。
 * Token 无效或缺失时直接放行（不返回 401），由后续的 AuthenticationEntryPoint 统一处理未认证响应。
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtil redisUtil;

    private final Cache<String, LoginUser> caffeineCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(5000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头提取 Token
        String token = request.getHeader(JwtConstant.TOKEN_HEADER);

        // Token 缺失时直接放行，不在此处返回 401，
        // 由 SecurityConfig 中注册的 AuthenticationEntryPoint 统一处理未认证响应
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token 存在但校验失败（过期或签名错误），同样放行交由 AuthenticationEntryPoint 处理
        // Token 合法，从中解析出用户 ID
        String userId = JwtUtil.getSubject(token);
        if (userId == null || userId.isEmpty()){
            filterChain.doFilter(request, response);
            return;
        }

        // 仅在 SecurityContext 中尚无认证信息时才填充，避免重复设置
        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 根据 userId 从 Redis 中取出登录时缓存的 LoginUser（含用户信息和权限列表）
            LoginUser loginUser = caffeineCache.get(userId, k -> redisUtil.get(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, LoginUser.class));
            if (loginUser != null){
                // 构建认证令牌并写入 SecurityContext，后续 Controller 可通过 SecurityContextHolder 获取当前用户
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
