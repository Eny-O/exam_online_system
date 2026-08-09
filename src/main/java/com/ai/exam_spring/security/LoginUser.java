package com.ai.exam_spring.security;

import com.ai.exam_spring.pojo.entity.Permission;
import com.ai.exam_spring.pojo.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 用户详情实现
 * <p>
 * 封装了 User 实体和权限列表，作为 JWT 认证通过后的安全上下文主体。
 * 账号过期、锁定等状态暂不启用，统一返回 true。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private User user;
    private List<Permission> permissions;

    public LoginUser(User user, List<Permission> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    @JsonIgnore
    private List<GrantedAuthority> authorities;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        authorities = permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.getPermCode())).collect(Collectors.toList());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    //TODO 账号过期、锁定、凭证过期、禁用等功能暂不启用
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
}
