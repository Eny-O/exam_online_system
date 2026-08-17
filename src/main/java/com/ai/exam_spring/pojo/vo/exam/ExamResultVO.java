package com.ai.exam_spring.pojo.vo.exam;

import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "考试结果VO")
public class ExamResultVO {

    @Schema(description = "考试ID")
    private Integer examId;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "考试总分")
    private Double totalScore;

    @Schema(description = "考试时间")
    private LocalDateTime examTime;

    @Schema(description = "考试时长")
    private Integer examDuration;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "考试分数")
    private Double examScore;

    @Schema(description = "学生考试状态")
    private StudentExamEnum studentExamStatus;

}
