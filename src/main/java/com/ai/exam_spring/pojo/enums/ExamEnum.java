package com.ai.exam_spring.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ExamEnum {
    NOT_POST(0,  "未发布"),
    ALREADY_POST(1, "已发布"),
    NOT_START(2, "未开始"),
    ONGOING(3, "进行中"),
    ALREADY_END(4, "已结束"),
    PENDING_REVIEW(5, "待批阅"),
    REVIEW_FINISHED(6, "批阅完成");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String message;

    ExamEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    // 告诉 Spring：如果前端传进来的是数字，请用这个方法转成枚举
    @JsonCreator
    public static ExamEnum fromCode(Integer code) {
        if (code == null) return null;
        for (ExamEnum status : values()) {
            if (status.code.equals(code)) return status;
        }
        return null;
    }
}
