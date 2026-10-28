package com.ai.exam_spring.modules.user.pojo.dto;

import com.ai.exam_common.common.pojo.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "人员管理查询DTO")
public class UserQueryDTO extends PageQuery {

    @Schema(description = "姓名（模糊搜索）")
    private String realName;

    @Schema(description = "学号/工号")
    private String userId;

    @Schema(description = "用户类型：1-学生，2-教师，3-管理员")
    private Integer userType;

    @Schema(description = "状态：1-启用，2-禁用")
    private Integer status;

    @Schema(description = "班级ID")
    private Integer classId;
}
