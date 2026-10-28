package com.ai.exam_spring.config;

import com.ai.exam_common.common.security.JwtAuthenticationTokenFilter;
import com.ai.exam_common.common.security.LoginUserReader;
import com.ai.exam_common.common.security.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * common 层安全组件装配
 */
@Configuration
public class CommonSecurityConfig {

    @Bean
    public SecurityUtil securityUtil() {
        return new SecurityUtil();
    }

    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(LoginUserReader loginUserReader) {
        return new JwtAuthenticationTokenFilter(loginUserReader);
    }
}
