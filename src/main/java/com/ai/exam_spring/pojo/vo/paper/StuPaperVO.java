package com.ai.exam_spring.pojo.vo.paper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "学生试卷展示对象")
public class StuPaperVO {

    @Schema(description = "主键id")
    private Integer id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷题目列表")
    private List<StuPaperQuestionVO> stuPaperQuestionVOS;

}
