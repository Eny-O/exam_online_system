package com.ai.exam_spring.pojo.entity;

import com.ai.exam_spring.common.status.PaperStatus;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 * 试卷表
 * </p>
 *
 */
@Getter
@Setter
@Schema(description = "试卷的实体对象")
public class Paper {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 id")
    private Integer id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "试卷描述")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建者 id")
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改者 id")
    private Integer updaterId;

    @Schema(description = "试卷状态:DRAFT为草稿,PENDING_ENABLE为待启用,ALREADY_ENABLE为已启用")
    private PaperStatus status = PaperStatus.PENDING_ENABLE;

    @Schema(description = "版本号")
    @Version
    private Integer version = 1;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}