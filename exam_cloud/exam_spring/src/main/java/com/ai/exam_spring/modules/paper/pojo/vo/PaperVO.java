package com.ai.exam_spring.modules.paper.pojo.vo;

import com.ai.exam_common.common.status.PaperStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "试卷信息展示对象")
public class PaperVO extends BasePaperVO{

    @Schema(description = "创建教师姓名")
    private String teacherName;

    @Schema(description = "试卷状态:DRAFT为草稿,PENDING_ENABLE为待启用,ALREADY_ENABLE为已启用")
    private PaperStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
