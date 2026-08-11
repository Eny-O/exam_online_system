package com.ai.exam_spring.pojo.dto.user;

import com.ai.exam_spring.pojo.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学生分页条件查询参数")
public class StudentQueryDTO extends PageQuery {

    @Schema(description = "学生姓名")
    private String realName;

    @Schema(description = "学生学号")
    private String studentNo;

    @Schema(description = "学生班级")
    private Integer classId;
}
