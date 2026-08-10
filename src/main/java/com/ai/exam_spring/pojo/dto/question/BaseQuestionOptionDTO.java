package com.ai.exam_spring.pojo.dto.question;

import com.ai.exam_spring.pojo.enums.question.OptionLabelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "题目选项的基类")
public class BaseQuestionOptionDTO {
    @Schema(description = "选项标签（A/B/C/D/E/F）", example = "A")
    private OptionLabelEnum label;

    @Schema(description = "选项内容", example = "implements")
    private String content;
}
