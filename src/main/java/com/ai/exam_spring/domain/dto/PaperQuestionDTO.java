package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "试卷内题目信息请求对象")
public class PaperQuestionDTO {

    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "题目分数")
    private Integer score;
}
