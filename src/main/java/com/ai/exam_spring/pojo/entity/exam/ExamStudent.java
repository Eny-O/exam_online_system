package com.ai.exam_spring.pojo.entity.exam;

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
@Schema(description = "考试学生关联表对象")
public class ExamStudent {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键id")
    private Integer id;

    @Schema(description = "考试id")
    private Integer examId;

    @Schema(description = "学生id")
    private Integer studentId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建者id")
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
