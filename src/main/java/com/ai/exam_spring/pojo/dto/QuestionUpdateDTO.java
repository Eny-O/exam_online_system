package com.ai.exam_spring.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "题目完整信息请求对象")
public class QuestionUpdateDTO extends BaseQuestionDTO {

    @Schema(description = "题目ID", example = "1001")
    private Integer id;

    @Schema(description = "题目的选项")
    private List<QuestionOptionUpdateDTO> options;
}
