package com.ai.exam_spring.config;

import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 枚举类型转换器配置
 * 解决 GET 请求查询参数（字符串）无法自动转换为枚举的问题
 */
@Configuration
public class EnumConvertConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, ExamEnum>() {
            @Override
            public ExamEnum convert(String source) {
                if (source == null || source.trim().isEmpty()) {
                    return null;
                }
                try {
                    int code = Integer.parseInt(source.trim());
                    for (ExamEnum status : ExamEnum.values()) {
                        if (status.getCode() == code) {
                            return status;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字，尝试按枚举名称匹配
                    try {
                        return ExamEnum.valueOf(source.trim());
                    } catch (IllegalArgumentException ex) {
                        // 忽略，返回 null
                    }
                }
                return null;
            }
        });
    }
}
