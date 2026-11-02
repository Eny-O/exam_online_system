package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 参考答案采分点覆盖情况：初评先把参考答案逐条拆成采分点清单，再说明每条由哪个评分要点覆盖，
 * 供护栏自动检测"采分点被漏拆"与"可选项被当成必答项扣分"
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "参考答案采分点覆盖情况")
public class ReferencePointCoverage {

    @Description("该采分点所属的要求部分，例如“核心设计原则”“GET 与 POST 的区别”；同一题目不同要求部分必须用不同取值")
    @Schema(description = "所属要求部分")
    private String aspect;

    @Description("该采分点的内容，即从参考答案中拆出的一条独立采分点")
    @Schema(description = "采分点内容")
    private String pointDesc;

    @Description("参考答案中该条是否标注为可选（如标注“（可选）”）；可选条目不因学生未作答而扣分")
    @Schema(description = "是否为可选项")
    private Boolean optional;

    @Description("覆盖该采分点的评分要点序号列表，对应 rubricPoints 的 pointNo")
    @Schema(description = "覆盖该采分点的评分要点序号")
    private List<Integer> coveredByPointNos;
}
