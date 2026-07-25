package com.ai.exam_spring.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询请求参数")
public class PageQuery {
    @Schema(description = "当前页码，默认第1页", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页显示条数，默认10条", example = "10")
    private Integer pageSize = 10;
}
