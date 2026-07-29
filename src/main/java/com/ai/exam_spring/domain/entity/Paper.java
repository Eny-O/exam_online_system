package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 试卷表
 * </p>
 *
 */
@TableName("paper")
@Getter
@Setter
@Schema(description = "试卷的实体对象")
public class Paper {
    @Schema(description = "主键id")
    private Long id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷描述")
    private String description;

    @Schema(description = "创建教师ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}