package com.ai.exam_auth_user.pojo.dos;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权限表
 */
@TableName("sys_permission")
@Getter
@Setter
@Schema(description = "权限/菜单实体对象")
public class Permission {

    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "父级ID（顶级菜单为0）", example = "0")
    private Integer parentId;

    @Schema(description = "权限编码（如：sys:user:add）", example = "sys:user:add")
    private String permCode;

    @Schema(description = "权限/菜单名称", example = "用户管理")
    private String permName;

    @Schema(description = "权限类型：1-目录，2-菜单，3-按钮，4-API", example = "2")
    private Integer type;

    @Schema(description = "前端路由路径", example = "/system/user")
    private String path;

    @Schema(description = "菜单图标（Element Plus 图标组件名）", example = "User")
    private String icon;

    @Schema(description = "前端组件路径", example = "system/user/index")
    private String component;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2024-06-01 12:00:00")
    private LocalDateTime updateTime;
}
