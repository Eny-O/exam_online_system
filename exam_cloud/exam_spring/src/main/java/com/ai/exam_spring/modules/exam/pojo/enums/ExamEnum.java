package com.ai.exam_spring.modules.exam.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.List;

@Getter
public enum ExamEnum {
    NOT_POST(0,  "未发布"),
    ALREADY_POST(1, "已发布"),
    NOT_START(2, "未开始"),
    ONGOING(3, "进行中"),
    PENDING_REVIEW(4, "待批阅"),
    REVIEW_FINISHED(5, "批阅完成");

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

    public List<ExamEnum> getAllowedTargets() {
        return switch (this) {
            case NOT_POST -> List.of(ALREADY_POST);
            case ALREADY_POST -> List.of(NOT_START, PENDING_REVIEW);
            case NOT_START -> List.of(ALREADY_POST, PENDING_REVIEW);
            case ONGOING -> List.of(PENDING_REVIEW);
            case PENDING_REVIEW -> List.of(REVIEW_FINISHED);
            case REVIEW_FINISHED -> List.of();
        };
    }

    /**
     * 校验是否允许流转到目标状态
     */
    public boolean canTransferTo(ExamEnum target) {
        return getAllowedTargets().contains(target);
    }

    /**
     * 判断枚举类型是否存在
     * @param code
     * @return
     */
    public static boolean exists(Integer code) {
        for (ExamEnum e : values()) {
            if (e.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
