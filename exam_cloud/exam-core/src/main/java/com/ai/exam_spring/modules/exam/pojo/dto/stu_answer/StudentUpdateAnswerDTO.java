package com.ai.exam_spring.modules.exam.pojo.dto.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "学生作答请求对象")
public class StudentUpdateAnswerDTO {

    @NotNull(message = "{validate.stu.answer.examId.notNull}")
    @Schema(description = "考试ID")
    private Integer examId;

    @NotNull(message = "{validate.stu.answer.questionId.notNull}")
    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;
}
