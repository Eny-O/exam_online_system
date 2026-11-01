package com.ai.exam_ai.config;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_common.common.security.JwtAuthenticationTokenFilter;
import com.ai.exam_common.common.security.LoginUserReader;
import com.ai.exam_common.common.security.LoginUserSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * exam_ai 安全配置：对齐 exam-core 的"无状态 + JWT + 自定义 401/403"模式。
 * 缺这份配置时 Spring Boot 默认安全会把未认证请求 302 跳 /login，导致网关转发后浏览器拿到 302。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 最小 LoginUserReader：exam_ai 不引 Redis，AI 接口也不做角色鉴权，
     * 只需把 JWT 里的 userId 还原为登录主体（空权限）让请求通过认证即可。
     */
    @Bean
    public LoginUserReader loginUserReader() {
        return userId -> {
            if (userId == null || userId.isEmpty()) {
                return null;
            }
            try {
                LoginUserSnapshot snapshot = new LoginUserSnapshot();
                snapshot.setId(Integer.valueOf(userId));
                snapshot.setUsername(userId);
                snapshot.setStatus(1);
                snapshot.setPermissions(List.of());
                return snapshot;
            } catch (NumberFormatException e) {
                return null;
            }
        };
    }

    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(LoginUserReader loginUserReader) {
        return new JwtAuthenticationTokenFilter(loginUserReader);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ResponseResult.error(ErrorCodeEnum.UNAUTHORIZED)));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ResponseResult.error(ErrorCodeEnum.FORBIDDEN)));
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**",
                                "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/ai/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults());
        return http.build();
    }
}
