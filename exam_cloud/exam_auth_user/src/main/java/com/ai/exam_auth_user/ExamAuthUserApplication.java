package com.ai.exam_auth_user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 认证与用户服务启动类
 * <p>
 * 负责 sys_user / sys_class / sys_permission / sys_role 相关能力，
 * 对外提供登录、登出、用户信息与权限查询接口。
 */
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.ai.exam_auth_user.mapper")
public class ExamAuthUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamAuthUserApplication.class, args);
    }
}
