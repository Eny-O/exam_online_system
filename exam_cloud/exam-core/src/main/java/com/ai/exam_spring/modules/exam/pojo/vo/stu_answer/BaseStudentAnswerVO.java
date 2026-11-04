package com.ai.exam_spring.modules.exam.pojo.vo.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生答案的基类VO")
public class BaseStudentAnswerVO {
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "题目ID", example = "303")
    private Integer questionId;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)", example = "1")
    private Integer isCorrect;

    @Schema(description = "得分", example = "9.5")
    private Double score;
}
