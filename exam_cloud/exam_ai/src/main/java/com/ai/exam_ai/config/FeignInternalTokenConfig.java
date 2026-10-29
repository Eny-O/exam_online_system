package com.ai.exam_ai.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 拦截器：为内部服务调用附加 X-Internal-Token 鉴权头
 */
@Configuration
public class FeignInternalTokenConfig {

    @Value("${exam.internal-api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                String targetName = template.feignTarget().name();
                if ("exam-core".equals(targetName) || "exam-auth-user".equals(targetName)) {
                    template.header("X-Internal-Token", internalApiKey);
                }
            }
        };
    }
}
