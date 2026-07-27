package com.ai.exam_spring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 在线考试系统启动类
 */
@SpringBootApplication
@MapperScan("com.ai.exam_spring.mapper")
public class ExamSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamSpringApplication.class, args);
    }

}
