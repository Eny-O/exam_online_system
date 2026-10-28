package com.ai.exam_spring.modules.exam.pojo.dto;

import com.ai.exam_spring.common.pojo.dto.PageQuery;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "考试分页条件查询请求对象")
public class ExamQueryDTO extends PageQuery {

    @Schema(description = "考试名称", example = "Java考试")
    private String examName;


    @Schema(description = "考试状态", example = "1",type = "integer")
    private ExamEnum status;

}
