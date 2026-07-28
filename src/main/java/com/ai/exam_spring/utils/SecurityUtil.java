package com.ai.exam_spring.utils;

import com.ai.exam_spring.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类
 */
public class SecurityUtil {
    /**
     * 获取当前登录用户的ID
     * @return 当前登录用户的ID
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUser().getId().toString();
        }
        return null;
    }

}
