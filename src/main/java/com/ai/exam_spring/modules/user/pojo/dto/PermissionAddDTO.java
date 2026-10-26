package com.ai.exam_spring.modules.user.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "权限新增DTO")
public class PermissionAddDTO {

    @NotNull(message = "父级ID不能为空")
    @Schema(description = "父级ID（顶级为0）")
    private Integer parentId;

    @NotBlank(message = "权限编码不能为空")
    @Schema(description = "权限编码")
    private String permCode;

    @NotBlank(message = "权限名称不能为空")
    @Schema(description = "权限名称")
    private String permName;

    @NotNull(message = "权限类型不能为空")
    @Schema(description = "权限类型：1-目录，2-菜单，3-按钮，4-API")
    private Integer type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "前端组件路径")
    private String component;
}
