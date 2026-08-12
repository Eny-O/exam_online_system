package com.ai.exam_spring.pojo.dto.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "学生作答请求对象")
public class StudentAnswerDTO {
    @NotNull(message = "{validate.exam.id.notNull}")
    @Schema(description = "考试ID", example = "101")
    private Integer examId;

    @NotNull(message = "{validate.paper.id.notNull}")
    @Schema(description = "试卷ID", example = "202")
    private Integer paperId;

    @NotEmpty(message = "{validate.stu.answer.list.notEmpty}")
    @Schema(description = "学生答案列表")
    private List<AnswerDTO> answers;
}
