package com.ai.exam_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 在线考试系统 API 网关
 * <p>
 * 职责：统一入口、路由转发（lb:// + Nacos 负载均衡）、跨域处理。
 * <p>
 * 第一版不做鉴权 —— Authorization 头原样透传，JWT 仍由后端 JwtAuthenticationTokenFilter 校验，
 * 保证后端零改动、迁移过程无回归风险。后续如需网关统一鉴权，再在此层前置校验逻辑。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ExamGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamGatewayApplication.class, args);
    }
}
