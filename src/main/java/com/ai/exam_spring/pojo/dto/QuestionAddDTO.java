package com.ai.exam_spring.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "题目添加请求对象")
public class QuestionAddDTO extends BaseQuestionDTO{
    @Schema(description = "题目的选项")
    private List<QuestionOptionAddDTO> options;
}
