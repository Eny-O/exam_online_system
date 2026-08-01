package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "试卷题目展示对象")
public class PaperQuestionVO {

    @Schema(description = "题序")
    private Integer sortOrder;

    @Schema(description = "题目分数")
    private Integer score;

    @Schema(description = "完整题目")
    private QuestionCompleteVO questionCompleteVO;

}
