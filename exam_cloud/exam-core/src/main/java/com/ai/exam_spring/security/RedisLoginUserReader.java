package com.ai.exam_spring.security;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.security.LoginUserPrincipal;
import com.ai.exam_common.common.security.LoginUserReader;
import com.ai.exam_common.common.security.LoginUserSnapshot;
import com.ai.exam_spring.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的登录主体读取实现
 * <p>
 * 与 auth-user 共用同一份 Redis 登录态缓存（key: login:{userId}），
 * 本服务直接从缓存还原登录主体，无需为每个请求远程调用 auth-user。
 * <p>
 * 写入方是 auth-user（登录时写入 LoginUserSnapshot），
 * 本类只读，不参与写入。
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
        return redisUtil.get(RedisConstant.LOGIN_USER_PREFIX_KEY + userId, LoginUserSnapshot.class);
    }
}
