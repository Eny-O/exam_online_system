package com.ai.exam_spring.pojo.dto.paper;

import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.pojo.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "试卷条件分页查询请求对象")
public class PaperQueryDTO extends PageQuery {

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "试卷状态")
    private PaperStatus paperStatus;
}
