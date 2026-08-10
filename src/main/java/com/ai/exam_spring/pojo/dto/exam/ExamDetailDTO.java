package com.ai.exam_spring.pojo.dto.exam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "考试详情请求对象")
public class ExamDetailDTO {

    @NotBlank(message = "{validate.exam.name.notBlank}")
    @Schema(description = "考试名称")
    private String examName;

    @NotNull(message = "{validate.paper.id.notNull}")
    @Schema(description = "关联试卷ID")
    private Integer paperId;

    @NotNull(message = "{validate.class.id.notNull}")
    @Schema(description = "参与班级id")
    private List<Integer> classIds;

    @NotNull(message = "{validate.exam.startTime.notNull}")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "{validate.exam.duration.notNull}")
    @Min(value = 0, message = "{validate.exam.duration.min}")
    @Schema(description = "考试时长(分钟)")
    private Integer duration;


}
