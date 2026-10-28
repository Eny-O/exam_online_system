package com.ai.exam_spring.modules.exam.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum StudentExamEnum {
    PENDING("pending", "待考试"),
    SUBMITTED("submitted", "已交卷"),
    GRADED("graded", "已批阅"),
    ABSENT("absent", "缺考");

    @JsonValue
    @EnumValue
    private final String code;
    private final String message;

    StudentExamEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
