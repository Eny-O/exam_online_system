package com.ai.exam_spring.modules.exam.pojo.vo.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生作答信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "学生作答信息")
public class StudentAnswerVO extends BaseStudentAnswerVO {
}
