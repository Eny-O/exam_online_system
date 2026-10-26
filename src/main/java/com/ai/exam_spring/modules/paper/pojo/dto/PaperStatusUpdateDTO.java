package com.ai.exam_spring.modules.paper.pojo.dto;

import com.ai.exam_spring.common.status.PaperStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "试卷状态信息请求对象")
public class PaperStatusUpdateDTO {

    @NotNull(message = "{validate.updatePaper.id.notNull}")
    @Schema(description = "试卷ID")
    private Integer id;

    @NotNull(message = "{validate.updatePaper.status.notNull}")
    @Schema(description = "试卷状态")
    private PaperStatus status;
}
