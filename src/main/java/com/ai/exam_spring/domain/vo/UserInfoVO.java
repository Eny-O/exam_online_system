package com.ai.exam_spring.domain.vo;

import com.ai.exam_spring.domain.entity.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 用户信息展示对象
 * <p>
 * 用于登录后返回用户基本信息和权限列表，密码等敏感字段不包含在内。
 */
@Data
@Schema(description = "用户信息展示对象")
public class UserInfoVO {

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "班级ID（管理员为null）", example = "101")
    private Integer classId;

    @Schema(description = "用户状态：1-正常, 0-禁用", example = "1")
    private Integer status;

    @Schema(description = "用户权限列表")
    private List<PermissionVO> permissions;
}
