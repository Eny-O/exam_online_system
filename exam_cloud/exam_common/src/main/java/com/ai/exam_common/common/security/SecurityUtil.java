package com.ai.exam_common.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类
 * <p>
 * 依赖 common 层的中立主体 {@link LoginUserPrincipal}，不感知具体业务模块，
 * 未来各服务可直接复用。
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户的ID
     * @return 当前登录用户的ID；未登录时返回 null
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserPrincipal principal) {
            Integer userId = principal.getId();
            return userId == null ? null : userId.toString();
        }
        return null;
    }
}

