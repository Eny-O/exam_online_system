package com.ai.exam_spring.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "试卷请求对象的基类")
public class BasePaperDTO {
    @NotBlank(message = "{validate.paper.name.notBlank}")
    @Schema(description = "试卷名称")
    private String name;

    @NotNull(message = "{validate.paper.totalScore.notNull}")
    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷描述")
    private String description;

    @NotEmpty(message = "{validate.paper.content.notEmpty}")
    @Schema(description = "试卷内容")
    List<PaperQuestionDTO> questions;
}
