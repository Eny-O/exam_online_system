package com.ai.exam_spring.pojo.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "权限树形VO")
public class PermissionTreeVO {

    @Schema(description = "权限ID")
    private Integer id;

    @Schema(description = "父级ID")
    private Integer parentId;

    @Schema(description = "权限编码")
    private String permCode;

    @Schema(description = "权限名称")
    private String permName;

    @Schema(description = "权限类型：1-目录，2-菜单，3-按钮，4-API")
    private Integer type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子级权限列表")
    private List<PermissionTreeVO> children;
}
