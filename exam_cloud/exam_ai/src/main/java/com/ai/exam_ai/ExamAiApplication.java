package com.ai.exam_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.ai.exam_api.feign")
public class ExamAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamAiApplication.class, args);
    }

}
