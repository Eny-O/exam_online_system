package com.ai.exam_auth_user.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 内部令牌拦截器：发往 exam-core 的请求携带 X-Internal-Token
 */
@Configuration
public class FeignInternalTokenConfig {

    @Value("${exam.internal-api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return (RequestTemplate template) -> {
            if ("exam-core".equals(template.feignTarget().name())) {
                template.header("X-Internal-Token", internalApiKey);
            }
        };
    }
}
