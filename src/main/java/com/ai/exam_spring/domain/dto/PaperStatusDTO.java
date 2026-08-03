package com.ai.exam_spring.domain.dto;

import com.ai.exam_spring.common.status.PaperStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "试卷状态信息请求对象")
public class PaperStatusDTO {
    @Schema(description = "试卷ID")
    private Integer id;


    @Schema(description = "试卷状态")
    private PaperStatus status;
}
