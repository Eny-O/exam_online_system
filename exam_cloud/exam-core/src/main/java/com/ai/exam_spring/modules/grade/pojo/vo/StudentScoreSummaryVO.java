package com.ai.exam_spring.modules.grade.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "学生成绩汇总")
public class StudentScoreSummaryVO implements Serializable {

    @Schema(description = "学生ID")
    private Integer studentId;

    @Schema(description = "客观题得分")
    private Double objectiveScore;

    @Schema(description = "主观题得分")
    private Double subjectiveScore;

    @Schema(description = "总分")
    private Double totalScore;
}
