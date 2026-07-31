package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 * 题目选项表
 * </p>
 *
 * @author HP
 * @since 2026-07-23
 */
@TableName("question_option")
@Getter
@Setter
@Schema(description = "题目选项实体对象")
public class QuestionOption {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "选项主键ID", example = "1")
    private Integer id;

    @Schema(description = "所属题目ID", example = "1001")
    private Integer questionId;

    @Schema(description = "选项标签（A/B/C/D/E/F/G/H）", example = "A")
    private String label;

    @Schema(description = "选项内容", example = "implements")
    private String content;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}