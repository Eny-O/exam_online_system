package com.ai.exam_spring.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "权限信息展示对象")
public class PermissionVO {

    @Schema(description = "父级ID（顶级菜单为0）", example = "0")
    private Integer parentId;

    @Schema(description = "权限编码（如：sys:user:add）", example = "sys:user:add")
    private String permCode;

    @Schema(description = "权限/菜单名称", example = "用户管理")
    private String permName;

    @Schema(description = "权限类型：1-目录，2-菜单，3-按钮", example = "2")
    private Integer type;

    @Schema(description = "前端路由路径", example = "/system/user")
    private String path;

    @Schema(description = "菜单图标（Element Plus 图标组件名）", example = "User")
    private String icon;

    @Schema(description = "前端组件路径", example = "system/user/index")
    private String component;
}
