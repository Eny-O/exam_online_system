package com.ai.exam_common.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.util.Collection;
import java.util.List;

/**
 * 登录态快照：写入 Redis 的登录主体载体
 * <p>
 * 认证服务（auth-user）登录成功后把主体快照写入 Redis；
 * 业务服务（exam-core）的 JWT 过滤器读取快照还原身份。
 * 该类只承载跨服务必需的字段，不依赖任何业务 DO，避免 common 反向依赖业务模块。
 */
@Data
public class LoginUserSnapshot implements LoginUserPrincipal {

    /** 用户ID */
    private Integer id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 学号（学生）或工号（教师） */
    private String studentNo;

    /** 用户类型：1-学生，2-教师，3-管理员 */
    private Integer userType;

    /** 用户状态：1-启用，2-禁用 */
    private Integer status;

    /** 权限编码列表（如 sys:user:add） */
    private List<String> permissions;

    @JsonIgnore
    private List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        if (permissions == null || permissions.isEmpty()) {
            authorities = List.of();
            return authorities;
        }
        authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        return authorities;
    }

    @Override
    public String getPassword() {
        // 快照不携带密码，认证凭据只在 auth-user 内部使用
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<String> getPermissionCodes() {
        return permissions == null ? List.of() : permissions;
    }



}
