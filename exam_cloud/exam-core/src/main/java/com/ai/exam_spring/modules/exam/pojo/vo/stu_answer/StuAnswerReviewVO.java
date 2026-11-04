package com.ai.exam_spring.modules.exam.pojo.vo.stu_answer;

import com.ai.exam_spring.modules.question.pojo.vo.QuestionOptionVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 学生答卷回顾：题目（含标准答案与解析）+ 学生作答合并展示
 */
@Data
@Schema(description = "学生答卷回顾：题目（含标准答案与解析）+ 学生作答")
public class StuAnswerReviewVO {

    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答")
    private String type;

    @Schema(description = "题干内容")
    private String content;

    @Schema(description = "题目选项列表")
    private List<QuestionOptionVO> options;

    @Schema(description = "正确答案")
    private String correctAnswer;

    @Schema(description = "题目解析")
    private String explanation;

    @Schema(description = "题目分值")
    private Double fullScore;

    @Schema(description = "学生作答内容")
    private String studentAnswer;

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)")
    private Integer isCorrect;

    @Schema(description = "学生得分")
    private Double score;

    @Schema(description = "教师批注")
    private String teacherComment;
}
