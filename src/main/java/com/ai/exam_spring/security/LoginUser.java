package com.ai.exam_spring.security;

import com.ai.exam_spring.domain.entity.Permission;
import com.ai.exam_spring.domain.entity.User;
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

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private User user; //用户信息
    private List<Permission> permissions; //权限信息

    public LoginUser(User user, List<Permission> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    @JsonIgnore
    private List<GrantedAuthority> authorities; //权限集合
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 判断权限集合是否为空
        if (authorities != null) {
            return authorities;
        }
        // 创建权限集合
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
