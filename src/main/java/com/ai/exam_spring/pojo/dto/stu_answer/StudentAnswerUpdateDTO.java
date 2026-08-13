package com.ai.exam_spring.pojo.dto.stu_answer;

import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@Schema(description = "学生得分更新请求对象")
public class StudentAnswerUpdateDTO {

    @NotNull(message = "{validate.stu.answer.primary.key.id.notNull}")
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @NotNull(message = "{validate.exam.id.notNull}")
    @Schema(description = "考试ID", example = "101")
    private Integer examId;

    @NotNull(message = "{validate.stu.id.notNull}")
    @Schema(description = "学生ID", example = "202")
    private Integer studentId;

    @NotNull(message = "{validate.stu.answer.questionId.notNull}")
    @Schema(description = "题目ID", example = "303")
    private Integer questionId;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答", example = "SINGLE")
    private QuestionTypeEnum type;

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)", example = "1")
    private Integer isCorrect;

    @Schema(description = "得分", example = "9.5")
    private Double score;
}

