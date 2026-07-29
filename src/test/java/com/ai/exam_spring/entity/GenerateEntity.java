package com.ai.exam_spring.entity;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.baomidou.mybatisplus.generator.config.TemplateType;

@SpringBootTest
public class GenerateEntity {
    @Test
    void generate() {
        FastAutoGenerator.create(
                        "jdbc:mysql://localhost:3306/exam_online_system?useUnicode=true&characterEncoding=utf-8",
                        "root", "123456"
                )
                .globalConfig(builder -> builder
                        .outputDir("src/main/java")          // 输出目录
                        .author("HP")                         // 作者注释
                        .disableOpenDir()                     // 生成后不自动打开文件夹
                )
                .packageConfig(builder -> builder
                        .parent("com.ai.exam_spring")         // 父包名
                        .entity("domain.entity")                     // 实体类包
                )
                .strategyConfig(builder -> builder
                        .addInclude("paper")
                        .entityBuilder()
                        .enableLombok()                   // 用 Lombok 简化 getter/setter
                        .enableTableFieldAnnotation()     // 加 @TableField 注解
                )
                .templateConfig(builder -> builder
                        .entity("templates/entity.java")
                        .disable(TemplateType.SERVICE, TemplateType.CONTROLLER, TemplateType.MAPPER)  // 如果只需要实体类，关掉多余的
                )
                .execute();
    }
}
