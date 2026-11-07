package com.ai.exam_api.trace;

import feign.RequestInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({OncePerRequestFilter.class, RequestInterceptor.class})
public class TraceIdLinkAutoConfiguration {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Bean
    public FilterRegistrationBean<TraceIdServletFilter> traceIdServletFilter() {
        FilterRegistrationBean<TraceIdServletFilter> registration =
                new FilterRegistrationBean<>(new TraceIdServletFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public RequestInterceptor traceIdFeignInterceptor() {
        return template -> {
            String traceId = MDC.get(TRACE_ID_MDC_KEY);
            if (StringUtils.hasText(traceId)) {
                template.header(TRACE_ID_HEADER, traceId);
            }
        };
    }

    static class TraceIdServletFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(TRACE_ID_MDC_KEY);
            }
        }
    }
}
