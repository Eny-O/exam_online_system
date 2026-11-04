package com.ai.exam_spring.modules.exam.pojo.vo;

import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 学生考试试卷视图：包含考试信息与该场考试的试卷内容
 */
@Data
@Schema(description = "学生考试试卷视图")
public class StuExamPaperVO {

    @Schema(description = "考试信息")
    private StuExamVO exam;

    @Schema(description = "试卷内容")
    private StuPaperVO paper;
}
