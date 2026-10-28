package com.ai.exam_spring.modules.grade.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "批阅的学生信息")
public class GradeStudentVO {

    @Schema(description = "学生ID")
    private Integer studentId;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "学生学号")
    private String studentNo;
}
