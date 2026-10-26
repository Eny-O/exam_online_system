package com.ai.exam_spring.modules.exam.pojo.dto;

import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "考试状态更改请求对象")
public class ExamStatusUpdateDTO {

    @NotNull(message = "{validate.exam.id.notNull}")
    @Schema(description = "考试id")
    private Integer id;

    @NotNull(message = "{validate.exam.status.notNull}")
    @Schema(description = "考试状态")
    private ExamEnum status;

    public ExamStatusUpdateDTO(Integer examId, ExamEnum examEnum) {
        this.id = examId;
        this.status = examEnum;
    }
}
