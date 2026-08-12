package com.ai.exam_spring.pojo.dto.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "作答请求对象")
public class AnswerDTO {

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "题目ID", example = "303")
    private Integer questionId;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;
}
