package com.ai.exam_spring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 在线考试系统启动类
 */
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ai.exam_api.feign")
@SpringBootApplication
@MapperScan("com.ai.exam_spring.modules.*.mapper")
public class ExamCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamCoreApplication.class, args);
    }

}
