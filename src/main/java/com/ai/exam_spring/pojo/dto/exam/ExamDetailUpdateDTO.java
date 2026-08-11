package com.ai.exam_spring.pojo.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "考试详情更新请求对象")
public class ExamDetailUpdateDTO extends BaseExamDetailDTO{
    @NotNull(message = "{validate.exam.id.notNull}")
    @Schema(description = "考试ID")
    private Integer id;
}
