package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("exam_class")
@Schema(description = "考试班级关联表")
public class ExamClass {
    @Schema(description = "主键 id")
    private Integer id;

    @Schema(description = "考试 ID")
    private Integer examId;

    @Schema(description = "班级 ID")
    private Integer classId;

    public ExamClass(Integer examId, Integer classId) {
        this.examId = examId;
        this.classId = classId;
    }



}
