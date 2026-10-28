package com.ai.exam_common.common.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface LoginUserPrincipal extends UserDetails {

    /** 用户ID */
    Integer getId();

    /** 真实姓名 */
    String getRealName();

    /** 班级ID（管理员为 null） */
    Integer getClassId();

    /** 用户状态：1-启用，2-禁用 */
    Integer getStatus();

    /** 权限编码列表（如 sys:user:add） */
    List<String> getPermissionCodes();

}
