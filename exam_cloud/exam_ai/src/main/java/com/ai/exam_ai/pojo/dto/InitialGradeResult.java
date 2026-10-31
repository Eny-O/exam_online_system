package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 初评结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "初评结果")
public class InitialGradeResult {

    @Description("初评状态。GRADED 表示已完成初评；UNGRADABLE 表示题目或作答信息不足，无法批阅，此时不要臆造分数")
    @Schema(description = "初评状态")
    private String status;

    @Description("拆解出的评分要点列表，各要点满分之和等于题目满分")
    @Schema(description = "评分要点列表")
    private List<RubricPoint> rubricPoints;

    @Description("初评总分，等于各要点得分之和")
    @Schema(description = "初评总分")
    private Double preliminaryScore;

    @Description("信息不足、无法判定的要点说明；为空表示判定依据充分")
    @Schema(description = "无法判定的要点说明")
    private List<String> unclearPoints;

    @Description("作答中出现的异常情况，例如跑题、照抄参考答案、明显是模板套话；没有问题则为空列表")
    @Schema(description = "作答异常情况")
    private List<String> concerns;

    @Description("整份初评的置信度，0到1之间")
    @Schema(description = "整体置信度")
    private Double confidence;
}
