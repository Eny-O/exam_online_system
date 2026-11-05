package com.ai.exam_spring.modules.exam.pojo.dos.stu_answer;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "学生答案表")
public class StudentAnswer {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "考试ID", example = "101")
    private Integer examId;

    @Schema(description = "学生ID", example = "202")
    private Integer studentId;

    @Schema(description = "题目ID", example = "303")
    private Integer questionId;

    @Schema(description = "学生作答内容", example = "A")
    private String studentAnswer;

    @Schema(description = "是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)", example = "1")
    private Integer isCorrect;

    @Schema(description = "得分", example = "9.5")
    private Double score;

    @Schema(description = "满分", example = "10.0")
    private Double fullScore;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答")
    private String questionType;

    @Schema(description = "教师批注(主观题)", example = "回答思路清晰，但缺少关键点。")
    private String teacherComment;

    @Schema(description = "批阅人ID(教师ID)", example = "505")
    private Integer graderId;

    @Schema(description = "批阅时间", example = "2026-08-12 14:30:00")
    private LocalDateTime gradeTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-08-12 13:00:00")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2026-08-12 14:30:00")
    private LocalDateTime updateTime;
}
