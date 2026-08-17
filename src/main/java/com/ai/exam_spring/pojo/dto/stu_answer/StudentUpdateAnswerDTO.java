package com.ai.exam_spring.pojo.dto.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "学生作答添加请求对象")
public class StudentUpdateAnswerDTO {

    @NotNull(message = "{validate.stu.answer.id.notNull}")
    @Schema(description = "studentAnswer的主键ID")
    private Integer id;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;
}
