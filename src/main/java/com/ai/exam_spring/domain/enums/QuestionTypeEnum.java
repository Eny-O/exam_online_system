package com.ai.exam_spring.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目类型枚举
 */
@Getter
@AllArgsConstructor
@Schema(description = "题目类型枚举")
public enum QuestionTypeEnum {
    @Schema(description = "单选题")
    SINGLE("single", "单选题"),

    @Schema(description = "多选题")
    MULTIPLE("multiple", "多选题"),

    @Schema(description = "判断题")
    JUDGE("judge", "判断题"),

    @Schema(description = "简答题")
    ESSAY("essay", "简答题");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;


    /**
     * 根据 code 获取对应的枚举值
     * @param code
     * @return
     */
    public static QuestionTypeEnum of(String code) {
        for (QuestionTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("无效的题目类型: " + code);
    }

}
