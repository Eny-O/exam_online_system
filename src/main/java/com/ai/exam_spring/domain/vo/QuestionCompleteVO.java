package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 题目完整信息展示对象（含答案、解析、选项）
 * <p>
 * 继承 QuestionVO 的基本字段，额外包含编辑所需的完整信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "题目完整信息展示对象")
public class QuestionCompleteVO extends QuestionVO {

    @Schema(description = "正确答案")
    private String answer;

    @Schema(description = "题目解析")
    private String analysis;

    @Schema(description = "题目选项列表")
    private List<QuestionOptionVO> options;
}
