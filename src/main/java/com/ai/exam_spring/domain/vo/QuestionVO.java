package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目列表展示对象
 * <p>
 * 用于分页查询返回题目简要信息，不包含选项和答案等详细内容。
 */
@Data
@Schema(description = "题目列表展示对象")
public class QuestionVO {
    @Schema(description = "题目ID")
    private Integer id;

    @Schema(description = "题干内容")
    private String content;

    @Schema(
            description = "题目类型: single-单选 multiple-多选 judge-判断 essay-简答",
            allowableValues = {"single", "multiple", "judge", "essay"}
    )
    private String type;

    @Schema(description = "所属知识点")
    private String knowledgePoint;

    @Schema(
            description = "难度: easy-medium-hard",
            allowableValues = {"easy", "medium", "hard"}
    )
    private String difficulty;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}