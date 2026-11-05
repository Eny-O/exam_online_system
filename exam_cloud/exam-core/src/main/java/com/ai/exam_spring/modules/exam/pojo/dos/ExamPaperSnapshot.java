package com.ai.exam_spring.modules.exam.pojo.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "考试试卷快照")
public class ExamPaperSnapshot {

    @TableId(value = "exam_id", type = IdType.INPUT)
    @Schema(description = "考试ID")
    private Integer examId;

    @Schema(description = "试卷ID")
    private Integer paperId;

    @Schema(description = "快照内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
