package com.ai.exam_spring.modules.grade.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批阅试卷内容VO")
public class GradePaperContentVO {

    @Schema(description = "主键id")
    private Integer id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Double totalScore;

    @Schema(description = "试卷题目列表")
    private List<GradePaperQuestionVO> gradePaperQuestionVOS;

}
