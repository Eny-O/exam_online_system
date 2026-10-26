package com.ai.exam_spring.modules.user.pojo.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_role")
@Data
@Schema(description = "角色实体对象")
public class Role {

    @TableId(type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Integer id;

    @Schema(description = "角色名称", example = "学生")
    private String roleName;

    @Schema(description = "角色编码", example = "ROLE_STUDENT")
    private String roleCode;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
