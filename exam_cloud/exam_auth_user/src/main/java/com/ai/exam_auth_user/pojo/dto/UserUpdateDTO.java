package com.ai.exam_auth_user.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改用户DTO")
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "学号（学生）")
    private String studentNo;

    @Schema(description = "工号（教师）")
    private String staffNo;

    @Schema(description = "班级ID（学生）")
    private Integer classId;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "状态：1-启用，2-禁用")
    private Integer status;
}
