package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "试卷和题目关联表")
@TableName("paper_question")
public class PaperQuestion {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "试卷ID")
    private Integer paperId;

    @Schema(description = "题目ID")
    private Integer questionId;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "题目分数")
    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
