package com.ai.exam_spring.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 题目列表展示对象
 * <p>
 * 用于分页查询返回题目简要信息，不包含选项和答案等详细内容。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "题目列表展示对象")
public class QuestionVO extends BaseQuestionVO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}