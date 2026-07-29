package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "试卷条件分页查询请求对象")
public class PaperQueryDTO extends PageQuery{

    @Schema(description = "试卷名称")
    private String name;
}
