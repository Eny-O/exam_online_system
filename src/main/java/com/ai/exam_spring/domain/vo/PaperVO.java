package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "试卷信息展示对象")
public class PaperVO {
    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷描述")
    private String description;

    @Schema(description = "创建教师姓名")
    private String teacherName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
