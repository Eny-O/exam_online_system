package com.ai.exam_auth_user.pojo.dto;

import com.ai.exam_common.common.pojo.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "班级分页查询参数")
public class ClassQueryDTO extends PageQuery {

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "年级")
    private String grade;
}
