package com.ai.exam_spring.pojo.vo.exam;

import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import com.ai.exam_spring.pojo.vo.paper.BasePaperVO;
import com.ai.exam_spring.pojo.vo.user.UStudentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "考试详情展示对象")
public class ExamDetailVO {
    @Schema(description = "考试ID")
    private Integer id;

    @Schema(description = "考试名称")
    private String examName;

    @Schema(description = "关联试卷")
    private Integer paperId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "考试时长(分钟)")
    private Integer duration;

    @Schema(description = "关联班级 id")
    private List<Integer> classIds;

    @Schema(description = "关联学生信息集合")
    private List<UStudentVO> studentVOS;

    @Schema(description = "试卷基本信息")
    private BasePaperVO basePaperVO;
}
