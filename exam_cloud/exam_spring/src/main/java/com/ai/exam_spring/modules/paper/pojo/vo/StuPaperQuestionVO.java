package com.ai.exam_spring.modules.paper.pojo.vo;

import com.ai.exam_spring.modules.question.pojo.vo.StuQuestionCompleteVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生试卷题目展示对象")
public class StuPaperQuestionVO {
    @Schema(description = "题序")
    private Integer sortOrder;

    @Schema(description = "题目分数")
    private Double score;

    @Schema(description = "完整题目")
    private StuQuestionCompleteVO stuQuestionCompleteVO;
}
