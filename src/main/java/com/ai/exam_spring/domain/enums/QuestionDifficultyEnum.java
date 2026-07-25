package com.ai.exam_spring.domain.enums;

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

    /**
     * 对应数据库存储的值 (easy/medium/hard)
     */
    private final String code;

    /**
     * 中文描述，用于前端展示或日志
     */
    private final String description;

    /**
     * 安全转换方法：将数据库/String值转为枚举对象
     *
     * @param code 数据库中的字符串值
     * @return 对应的枚举对象，若找不到则返回 null (避免抛异常导致查询中断)
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
        throw new IllegalArgumentException("无效的题目难度等级: " + code);
    }
}
