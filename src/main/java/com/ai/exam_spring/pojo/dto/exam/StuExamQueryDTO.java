package com.ai.exam_spring.pojo.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "学生查询考试请求对象")
public class StuExamQueryDTO extends ExamQueryDTO{

}
