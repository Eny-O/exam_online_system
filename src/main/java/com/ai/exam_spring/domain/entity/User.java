package com.ai.exam_spring.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * 用户实体，对应 sys_user 表
 */
@Getter
@Setter
@TableName("sys_user")
@Schema(description = "用户实体对象")
public class User {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @JsonIgnore // 序列化时忽略密码字段，防止密码泄露
    @Schema(description = "密码（接口返回时已隐藏）", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "班级ID（管理员为null）", example = "10")
    private Integer classId;

    @Schema(description = "用户状态：1-启用，2-禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-06-01 12:00:00")
    private LocalDateTime updateTime;
}
