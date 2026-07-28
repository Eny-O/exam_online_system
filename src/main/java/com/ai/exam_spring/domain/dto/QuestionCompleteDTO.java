package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "题目完整信息请求对象")
public class QuestionCompleteDTO {
    @Schema(description = "题目ID", example = "1001")
    private Long id;

    @NotBlank(message = "题目内容不能为空")
    @Schema(description = "题目内容", example = "Java中哪个关键字用于实现接口？")
    private String content;

    @NotBlank(message = "题目类型不能为空")
    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答", example = "single")
    private String type;

    @NotBlank(message = "题目难度不能为空")
    @Schema(description = "难度: easy-medium-hard", example = "medium")
    private String difficulty;

    @Schema(description = "题目所属知识点", example = "面向对象编程")
    private String knowledgePoint;

    @Schema(description = "正确答案(客观题存选项标签如A;判断题存true/false;简答题存参考答案)", example = "A")
    private String answer;

    @Schema(description = "题目解析", example = "在Java中，使用implements关键字来实现接口...")
    private String analysis;

    @Schema(description = "题目的选项")
    private List<QuestionOptionDTO> options;
}
