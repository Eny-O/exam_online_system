package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "试卷内容信息请求对象")
public class PaperContentDTO {
    @Schema(description = "试卷ID(新增不填,更新要填)")
    private Integer id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷描述")
    private String description;

    @Schema(description = "试卷内容")
    List<PaperQuestionDTO> questions;
}
