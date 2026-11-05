package com.ai.exam_spring.modules.exam.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "考试试卷快照内容")
public class ExamPaperSnapshotDTO {

    @Schema(description = "考试ID")
    private Integer examId;

    @Schema(description = "试卷ID")
    private Integer paperId;

    @Schema(description = "考试信息")
    private ExamInfo exam;

    @Schema(description = "试卷信息")
    private PaperInfo paper;

    @Schema(description = "题目列表")
    private List<QuestionInfo> questions;

    @Data
    @Schema(description = "考试信息")
    public static class ExamInfo {
        private String examName;
        private String startTime;
        private Integer duration;
    }

    @Data
    @Schema(description = "试卷信息")
    public static class PaperInfo {
        private String name;
        private Double totalScore;
        private String description;
    }

    @Data
    @Schema(description = "题目信息")
    public static class QuestionInfo {
        private Integer questionId;
        private Integer sortOrder;
        private Double score;
        private String content;
        private String type;
        private String knowledgePoint;
        private String difficulty;
        private String answer;
        private String analysis;
        private List<OptionInfo> options;
    }

    @Data
    @Schema(description = "选项信息")
    public static class OptionInfo {
        private Integer optionId;
        private String label;
        private String content;
    }
}
