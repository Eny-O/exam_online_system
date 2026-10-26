package com.ai.exam_spring.modules.exam.pojo.dos;

import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Schema(description = "考试实体对象")
@TableName("exam")
public class Exam {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "考试ID")
    private Integer id;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "关联试卷ID")
    private Integer paperId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长(分钟)")
    private Integer duration;

    @Schema(description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成,其中2,3,4前端维护")
    private ExamEnum status = ExamEnum.NOT_POST;


    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改者 id")
    private Integer updaterId;

    @Version
    @Schema(description = "版本号")
    private Integer version = 1;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
