package com.ai.exam_spring.modules.exam.pojo.vo.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生作答信息
 * 对错与得分等字段统一由基类 BaseStudentAnswerVO 承载，学生端与教师端返回结构保持一致
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "学生作答信息")
public class StudentAnswerVO extends BaseStudentAnswerVO {
}
