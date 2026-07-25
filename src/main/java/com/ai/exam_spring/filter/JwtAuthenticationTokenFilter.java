package com.ai.exam_spring.filter;

import com.ai.exam_spring.common.constant.JwtConstant;
import com.ai.exam_spring.common.constant.RedisConstant;
import com.ai.exam_spring.security.LoginUser;
import com.ai.exam_spring.utils.JwtUtil;
import com.ai.exam_spring.utils.RedisUtil;
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

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 拦截请求，进行 JWT 认证
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //获取token
        String token = request.getHeader(JwtConstant.TOKEN_HEADER);
        //判断token是否存在
        if (!StringUtils.hasText(token)) {
            //不存在放行
            filterChain.doFilter(request, response);
            return;
        }
        //判断token是否合法
        if (!JwtUtil.validateToken(token)){
            //不合法放行
            filterChain.doFilter(request, response);
            return;
        }
        //存在进行解析
        String userId = JwtUtil.getSubject(token);

        //如果存在并且上下文没有用户信息，则设置用户信息
        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            //从redis获取用户信息（key 格式与登录时存储保持一致："login" + userId）
            LoginUser loginUser = redisUtil.get(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, LoginUser.class);
            //判断该用户是否存在
            if (loginUser != null){
                //构建认证令牌，并设置到 Spring Security 上下文中
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        //放行
        filterChain.doFilter(request, response);
    }
}
