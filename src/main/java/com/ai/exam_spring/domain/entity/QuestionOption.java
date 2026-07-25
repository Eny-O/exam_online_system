package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

    @Schema(description = "选项主键ID", example = "1")
    private Long id;

    @Schema(description = "所属题目ID", example = "1001")
    @TableField("question_id")
    private Long questionId;

    @Schema(description = "选项标签（A/B/C/D/E/F/G/H）", example = "A")
    private String label;

    @Schema(description = "选项内容", example = "implements")
    private String content;
}