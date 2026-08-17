package com.ai.exam_spring.pojo.vo.stu_answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "学生作答信息")
public class StudentAnswerVO extends BaseStudentAnswerVO {

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)", example = "1")
    private Integer isCorrect;

    @Schema(description = "得分", example = "9.5")
    private Double score;

}
