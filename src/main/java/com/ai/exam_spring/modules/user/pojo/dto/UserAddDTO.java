package com.ai.exam_spring.modules.user.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "新增用户DTO")
public class UserAddDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "真实姓名")
    private String realName;

    @NotNull(message = "用户类型不能为空")
    @Schema(description = "用户类型：1-学生，2-教师，3-管理员")
    private Integer userType;

    @Schema(description = "学号（学生）")
    private String studentNo;

    @Schema(description = "工号（教师）")
    private String staffNo;

    @Schema(description = "班级ID（学生）")
    private Integer classId;

    @Schema(description = "专业")
    private String major;
}
