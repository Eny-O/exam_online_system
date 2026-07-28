package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目选项展示对象")
public class QuestionOptionVO {

    @Schema(description = "选项标签（A/B/C/D/E/F/G/H）", example = "A")
    private String label;

    @Schema(description = "选项内容", example = "implements")
    private String content;
}
