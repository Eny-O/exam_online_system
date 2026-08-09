package com.ai.exam_spring.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // 序列化统一使用带秒格式
            builder.serializers(new LocalDateTimeSerializer(FORMATTER_WITH_SECONDS));

            // 反序列化兼容多种格式
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText().trim();
                    // 尝试带秒格式
                    try {
                        return LocalDateTime.parse(text, FORMATTER_WITH_SECONDS);
                    } catch (DateTimeParseException e) {
                        // 尝试不带秒格式
                        try {
                            return LocalDateTime.parse(text, FORMATTER_WITHOUT_SECONDS);
                        } catch (DateTimeParseException e2) {
                            // 尝试 ISO 格式
                            return LocalDateTime.parse(text);
                        }
                    }
                }
            });
            builder.modules(module);
        };
    }
}
