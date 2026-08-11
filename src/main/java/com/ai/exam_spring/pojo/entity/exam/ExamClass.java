package com.ai.exam_spring.pojo.entity.exam;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("exam_class")
@Schema(description = "考试班级关联表")
public class ExamClass {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 id")
    private Integer id;

    @Schema(description = "考试 ID")
    private Integer examId;

    @Schema(description = "班级 ID")
    private Integer classId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建者id")
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public ExamClass(Integer examId, Integer classId) {
        this.examId = examId;
        this.classId = classId;
    }



}
