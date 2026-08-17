package com.ai.exam_spring.pojo.vo.exam;

import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "学生考试展示对象")
public class StuExamVO {
    @Schema(description = "考试ID")
    private Integer id;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长(分钟)")
    private Integer duration;

    @Schema(description = "状态: 2-未开始,3-进行中,4-待批阅,5-批阅完成")
    private ExamEnum status;

    @Schema(description = "试卷id")
    private Integer paperId;

    @Schema(description = "学生考试状态")
    private String studentExamStatus;

}
