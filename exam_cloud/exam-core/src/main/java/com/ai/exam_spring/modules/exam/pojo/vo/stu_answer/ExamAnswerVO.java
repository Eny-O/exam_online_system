package com.ai.exam_spring.modules.exam.pojo.vo.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "考试答案VO")
public class ExamAnswerVO {

    @Schema(description = "学生答案表主键id")
    private Integer id;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;

    @Schema(description = "正确答案")
    private String answer;

    @Schema(description = "满分")
    private Double fullScore;

    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答")
    private String type;
}
