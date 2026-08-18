package com.ai.exam_spring.pojo.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "人员管理列表VO")
public class UserManageVO {

    @Schema(description = "用户ID")
    private Integer id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "用户类型：1-学生，2-教师，3-管理员")
    private Integer userType;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "工号")
    private String staffNo;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "状态：1-启用，2-禁用")
    private Integer status;

    @Schema(description = "班级ID")
    private Integer classId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
