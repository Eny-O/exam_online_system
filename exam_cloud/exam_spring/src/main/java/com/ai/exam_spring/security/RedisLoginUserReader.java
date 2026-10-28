package com.ai.exam_spring.security;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.security.LoginUserPrincipal;
import com.ai.exam_common.common.security.LoginUserReader;

import com.ai.exam_spring.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的登录主体读取实现
 * <p>
 * 读取登录时缓存的 LoginUser，并返回其中立视图 LoginUserPrincipal。
 * 未来拆分为多服务后，可替换为 Feign 调用 auth-user 的实现，
 * common 侧的 JWT 过滤器无需任何改动。
 */
@Component
public class RedisLoginUserReader implements LoginUserReader {
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public LoginUserPrincipal read(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return redisUtil.get(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, LoginUser.class);
    }
}
