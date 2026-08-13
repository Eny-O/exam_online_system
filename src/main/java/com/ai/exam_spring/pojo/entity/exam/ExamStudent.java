package com.ai.exam_spring.pojo.entity.exam;

import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "考试学生关联表对象")
public class ExamStudent {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键id")
    private Integer id;

    @Schema(description = "考试id")
    private Integer examId;

    @Schema(description = "学生id")
    private Integer studentId;

    @Schema(description = "学生考试状态: pending-待考试,submitted-已交卷,grade-已批阅,absent-缺考")
    private StudentExamEnum status;

    @Schema(description = "客观题分数")
    private Double objectiveScore;

    @Schema(description = "主观题分数")
    private Double subjectiveScore;

    @Schema(description = "总分")
    private Double totalScore;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建者id")
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;


    public ExamStudent() {
    }
    public ExamStudent(Integer examId, Integer studentId) {
        this.examId = examId;
        this.studentId = studentId;
    }

}
