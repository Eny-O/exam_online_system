package com.ai.exam_spring.modules.paper.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "试卷内容信息请求对象")
public class PaperUpdateDTO extends BasePaperDTO {
    @NotNull(message = "{validate.paper.id.notNull}")
    @Schema(description = "试卷ID")
    private Integer id;

}
