package com.ai.exam_spring.pojo.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "考试详情添加请求对象")
public class ExamDetailAddDTO extends BaseExamDetailDTO {
}
