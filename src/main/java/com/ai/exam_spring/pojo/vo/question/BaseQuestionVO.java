package com.ai.exam_spring.pojo.vo.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目VO的基类")
public class BaseQuestionVO {

    @Schema(description = "题目ID")
    private Integer id;

    @Schema(description = "题干内容")
    private String content;

    @Schema(
            description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答",
            allowableValues = {"single", "multiple", "judge", "essay"}
    )
    private String type;

    @Schema(description = "所属知识点")
    private String knowledgePoint;

    @Schema(
            description = "难度: easy-medium-hard",
            allowableValues = {"easy", "medium", "hard"}
    )
    private String difficulty;
}
