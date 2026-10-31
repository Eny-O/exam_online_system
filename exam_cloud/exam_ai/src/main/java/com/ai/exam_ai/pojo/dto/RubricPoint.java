package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 评分要点
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "评分要点")
public class RubricPoint {

    @Description("要点序号，从1开始")
    @Schema(description = "要点序号")
    private Integer pointNo;

    @Description("该要点的内容描述，即参考答案拆解出的一个独立得分点")
    @Schema(description = "要点内容")
    private String pointDesc;

    @Description("该要点的满分值，所有要点满分之和等于题目满分")
    @Schema(description = "该要点满分")
    private Double maxScore;

    @Description("该要点的实际得分，0到满分之间，可为小数")
    @Schema(description = "该要点得分")
    private Double awardedScore;

    @Description("判定依据，必须引用学生作答中的原文片段；未命中时说明缺失什么")
    @Schema(description = "判定依据（引用学生作答原文）")
    private String evidence;

    @Description("本次判定的置信度，0到1之间；信息模糊无法判断时给低分")
    @Schema(description = "判定置信度")
    private Double confidence;

    @Description("该要点是否被完全命中")
    @Schema(description = "是否完全命中")
    private Boolean fullyHit;
}
