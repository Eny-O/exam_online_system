package com.ai.exam_spring.modules.question.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "增加选项请求对象")
public class QuestionOptionAddDTO extends BaseQuestionOptionDTO{
}
