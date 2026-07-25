package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询题目参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "题目查询请求参数")
public class QuestionQueryDTO extends PageQuery {

    @Schema(description = "题目内容关键词（模糊匹配）", example = "Java多线程")
    private String content;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答", allowableValues = {"single", "multiple", "judge", "essay"})
    private String type;

    @Schema(description = "难度: easy-medium-hard", allowableValues = {"easy", "medium", "hard"})
    private String difficulty;
}
