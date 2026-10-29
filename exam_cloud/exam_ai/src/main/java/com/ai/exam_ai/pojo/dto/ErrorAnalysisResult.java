package com.ai.exam_ai.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 错题解析结果
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "错题解析结果")
public class ErrorAnalysisResult {

    @Description("错因解析：说明学生为什么做错、暴露出的知识漏洞，不超过 300 字")
    @Schema(description = "错因解析")
    private String analysisText;

    @Description("该题反映出的薄弱知识点名称")
    @Schema(description = "薄弱知识点")
    private String knowledgeGap;

    @Description("针对该薄弱点的改进建议，不超过 200 字")
    @Schema(description = "改进建议")
    private String suggestion;
}
