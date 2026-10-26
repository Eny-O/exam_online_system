package com.ai.exam_spring.modules.question.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "题目完整信息请求对象")
public class QuestionUpdateDTO extends BaseQuestionDTO {

    @Schema(description = "题目ID", example = "1001")
    private Integer id;

    @Schema(description = "题目的选项")
    private List<QuestionOptionUpdateDTO> options;
}
