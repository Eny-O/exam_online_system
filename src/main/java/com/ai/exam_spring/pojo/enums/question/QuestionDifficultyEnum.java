package com.ai.exam_spring.pojo.enums.question;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 题目难度等级枚举
 * 对应数据库 difficulty 字段 (varchar)
 */
@Getter
@AllArgsConstructor
@Schema(description = "题目难度等级枚举")
public enum QuestionDifficultyEnum {

    @Schema(description = "简单")
    EASY("easy", "简单"),

    @Schema(description = "中等")
    MEDIUM("medium", "中等"),

    @Schema(description = "困难")
    HARD("hard", "困难");

    @JsonValue
    @EnumValue
    private final String code;
    private final String description;

    /**
     * 安全转换方法：将数据库/String值转为枚举对象
     *
     * @param code 数据库中的字符串值
     * @return 对应的枚举对象
     * @throws IllegalArgumentException 当 code 无效时抛出
     */
    public static QuestionDifficultyEnum of(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (QuestionDifficultyEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_LABEL_ERROR);
    }
}
