package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "考试详情请求对象")
public class ExamDetailDTO {
    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "关联试卷ID")
    private Integer paperId;

    @Schema(description = "参与班级id")
    private List<Integer> classIds;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长(分钟)")
    private Integer duration;


}
