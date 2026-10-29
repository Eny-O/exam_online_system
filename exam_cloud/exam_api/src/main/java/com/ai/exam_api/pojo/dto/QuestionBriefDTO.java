package com.ai.exam_api.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 题目摘要（跨服务传输）
 */
@Data
@Schema(description = "题目摘要")
public class QuestionBriefDTO {

    @Schema(description = "题目ID")
    private Integer id;

    @Schema(description = "题干")
    private String content;

    @Schema(description = "题型: single/multiple/judge/essay")
    private String type;

    @Schema(description = "知识点")
    private String knowledgePoint;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "解析")
    private String analysis;
}
