package com.ai.exam_auth_user.pojo.dos;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联实体，对应 sys_user_role 表
 */
@TableName("sys_user_role")
@Data
public class SysUserRole {

    private Integer userId;

    private Integer roleId;
}
