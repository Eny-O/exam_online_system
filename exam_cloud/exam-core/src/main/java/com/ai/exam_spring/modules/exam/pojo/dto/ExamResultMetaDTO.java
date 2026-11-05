package com.ai.exam_spring.modules.exam.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "成绩页使用的考试与试卷元数据")
public class ExamResultMetaDTO {

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "考试时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长")
    private Integer duration;

    @Schema(description = "试卷总分")
    private Double totalScore;
}
