package com.ai.exam_ai.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 主观题批阅请求
 */
@Data
@Schema(description = "主观题批阅请求")
public class SubjectiveGradeRequest {

    @Schema(description = "考试ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "考试ID不能为空")
    private Integer examId;

    @Schema(description = "学生ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "学生ID不能为空")
    private Integer studentId;

    @Schema(description = "题目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "题目ID不能为空")
    private Integer questionId;
}
