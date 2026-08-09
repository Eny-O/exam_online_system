package com.ai.exam_spring.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@Schema(description = "试卷内题目信息请求对象")
public class PaperQuestionDTO {

    @NotNull(message = "{validate.paper.question.questionId.notNull}")
    @Schema(description = "题目ID")
    private Integer questionId;

    @Min(value = 1, message = "{validate.paper.question.sortOrder.min}")
    @NotNull(message = "{validate.paper.question.sortOrder.notNull}")
    @Schema(description = "排序序号")
    private Integer sortOrder;

    @NotNull(message = "{validate.paper.question.score.notNull}")
    @Min(value = 1, message = "{validate.paper.question.score.min}")
    @Schema(description = "题目分数")
    private Integer score;
}
