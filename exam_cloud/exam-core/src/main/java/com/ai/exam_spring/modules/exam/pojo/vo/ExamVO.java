package com.ai.exam_spring.modules.exam.pojo.vo;

import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
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

    @Schema(description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成,其中2,3,4前端维护")
    private ExamEnum status;

    @Schema(description = "关联班级 id")
    private Integer classId;

    @Schema(description = "创建人ID（分库后用于跨服务补齐创建人姓名）")
    private Integer creatorId;

    @Schema(description = "创建人姓名")
    private String creatorName;
}
