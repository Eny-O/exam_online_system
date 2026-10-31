package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 再评对单个要点的裁决
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "再评逐点裁决")
public class PointDecision {

    @Description("对应的要点序号，必须与初评结果的要点一一对应")
    @Schema(description = "要点序号")
    private Integer pointNo;

    @Description("初评给出的该要点得分")
    @Schema(description = "初评得分")
    private Double initialScore;

    @Description("再评裁定的该要点最终得分，0到该要点满分之间")
    @Schema(description = "最终得分")
    private Double finalScore;

    @Description("裁决动作。KEEP 维持初评、UP 上调、DOWN 下调")
    @Schema(description = "裁决动作")
    private String action;

    @Description("裁决理由。必须具体说明依据哪一条偏差检查得出该结论")
    @Schema(description = "裁决理由")
    private String reason;

    @Description("本次裁决识别出的偏差类型列表，可为空列表")
    @Schema(description = "识别出的偏差类型")
    private List<String> biasFlags;

    @Description("该要点最终是否判定为完全命中")
    @Schema(description = "是否完全命中")
    private Boolean fullyHit;
}
