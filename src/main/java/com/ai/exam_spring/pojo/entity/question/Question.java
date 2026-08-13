package com.ai.exam_spring.pojo.entity.question;

import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 * 题目表
 * </p>
 *
 * @author HP
 * @since 2026-07-23
 */
@TableName("question")
@Getter
@Setter
@Schema(description = "题目实体对象")
public class Question {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "题目ID", example = "1001")
    private Integer id;

    @Schema(description = "题目内容", example = "Java中哪个关键字用于实现接口？")
    private String content;

    @Schema(description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答", example = "SINGLE")
    private QuestionTypeEnum type;

    @Schema(description = "难度: easy-medium-hard", example = "medium")
    private String difficulty;

    @Schema(description = "题目所属知识点", example = "面向对象编程")
    private String knowledgePoint;

    @Schema(description = "正确答案(客观题存选项标签如A;判断题存true/false;简答题存参考答案)", example = "A")
    private String answer;

    @Schema(description = "题目解析", example = "在Java中，使用implements关键字来实现接口...")
    private String analysis;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "题目创建者ID", example = "1")
    private Integer creatorId;

    @Version
    @Schema(description = "题目版本号", example = "1")
    private Integer version = 1;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "题目创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "题目更新时间", example = "2024-06-01 12:00:00")
    private LocalDateTime updateTime;
}
