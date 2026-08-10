package com.ai.exam_spring.pojo.vo.exam;

import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "考试分页展示对象")
public class ExamVO {
    @Schema(description = "考试ID")
    private Integer id;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "关联试卷")
    private String paperName;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长(分钟)")
    private Integer duration;

    @Schema(description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-已结束,5-待批阅,6-批阅完成")
    private ExamEnum status;

    @Schema(description = "关联班级 id")
    private Integer classId;
}
