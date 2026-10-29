package com.ai.exam_api.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 学生作答摘要（跨服务传输）
 */
@Data
@Schema(description = "学生作答摘要")
public class StudentAnswerDTO {

    @Schema(description = "作答记录ID")
    private Integer id;

    @Schema(description = "考试ID")
    private Integer examId;

    @Schema(description = "学生ID")
    private Integer studentId;

    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "学生作答内容")
    private String studentAnswer;

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判 2-半对")
    private Integer isCorrect;

    @Schema(description = "得分")
    private Double score;

    @Schema(description = "满分")
    private Double fullScore;

    @Schema(description = "教师批注")
    private String teacherComment;
}
