package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 主观题批阅结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "主观题批阅结果")
public class GradeResult {

    @Description("本次批阅给出的得分，取值范围 0 到该题满分，允许小数")
    @Schema(description = "得分")
    private Double score;

    @Description("学生作答是否完全正确；得分未达满分即为 false")
    @Schema(description = "是否完全正确")
    private Boolean isCorrect;

    @Description("面向学生的批阅评语，说明得分依据、优点与不足，不超过 200 字")
    @Schema(description = "批阅评语")
    private String comment;
}
