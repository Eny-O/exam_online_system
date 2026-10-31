package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 再评结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "再评结果")
public class ReviewGradeResult {

    @Description("再评状态。GRADED 表示已完成复核；UNGRADABLE 表示信息不足无法复核")
    @Schema(description = "再评状态")
    private String status;

    @Description("逐点裁决列表，必须覆盖初评的全部要点")
    @Schema(description = "逐点裁决列表")
    private List<PointDecision> decisions;

    @Description("复核后的最终总分，等于各要点最终得分之和")
    @Schema(description = "最终总分")
    private Double finalScore;

    @Description("本次复核识别出的全部偏差类型，可为空列表")
    @Schema(description = "识别出的偏差类型")
    private List<String> biasFlags;

    @Description("初评与再评的一致程度，0到1之间，1表示完全一致")
    @Schema(description = "两评一致度")
    private Double agreement;

    @Description("复核意见，说明本次复核的判断依据与结论，面向教师")
    @Schema(description = "复核意见")
    private String reviewComment;

    @Description("是否存在需要人工介入的情况，例如发现参考答案要点遗漏；没有则为空列表")
    @Schema(description = "需人工介入的情况")
    private List<String> escalations;
}
