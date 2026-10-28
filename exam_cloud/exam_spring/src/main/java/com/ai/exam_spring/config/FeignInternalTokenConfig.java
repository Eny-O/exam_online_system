package com.ai.exam_spring.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨服务 Feign 内部令牌拦截器。
 * <p>
 * 仅对发往 exam-auth-user 的 Feign 调用自动附加 {@code X-Internal-Token} 头，
 * 与 auth-user 侧 {@code InternalApiAuthFilter} 配套，使 core 能正常调用
 * {@code /inner/user/briefs}、{@code /inner/class/briefs} 等内部接口。
 * <p>
 * 拦截器放在 exam-spring（而非 exam_api）下，确保被 core 容器扫描到——
 * core 的包扫描根不覆盖 {@code com.ai.exam_api}。
 */
@Configuration
public class FeignInternalTokenConfig {

    @Value("${exam.internal-api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return (RequestTemplate template) -> {
            // 仅对 auth-user 这一目标附加内部令牌，避免泄露到其它 Feign 客户端
            if ("exam-auth-user".equals(template.feignTarget().name())) {
                template.header("X-Internal-Token", internalApiKey);
            }
        };
    }
}
