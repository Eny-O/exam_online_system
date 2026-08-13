package com.ai.exam_spring.pojo.vo.grade;

import com.ai.exam_spring.pojo.vo.question.QuestionOptionVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批阅试卷题目VO")
public class GradePaperQuestionVO {
    @Schema(description = "题序")
    private Integer sortOrder;

    @Schema(description = "题目分数")
    private Double score;

    @Schema(description = "题目ID")
    private Integer id;

    @Schema(description = "正确答案")
    private String correctAnswer;

    @Schema(description = "题干内容")
    private String content;

    @Schema(description = "题目类型")
    private String type;

    @Schema(description = "题目选项列表")
    private List<QuestionOptionVO> options;



}
