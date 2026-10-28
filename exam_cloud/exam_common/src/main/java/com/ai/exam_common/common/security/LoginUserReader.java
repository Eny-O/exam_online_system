package com.ai.exam_common.common.security;

public interface LoginUserReader {

    /**
     * 根据 userId 读取登录主体；不存在（未登录/已登出）返回 null。
     */
    LoginUserPrincipal read(String userId);
}
