package com.ai.exam_spring.common.constant;

public class JwtConstant {
    public static final String TOKEN_HEADER = "token";
    public static final String TOKEN_PREFIX = "Bearer ";
    // 默认签名密钥（生产环境建议从配置文件读取，且长度需满足 HMAC-SHA256 要求，至少 32 字节）
    public static final String SECRET_KEY = "+fyBfkGRq7yuOCOW+dd+4jqO8fz3uXS5B4IsQntGDDs=";

    // 默认 Token 过期时间（毫秒），这里设置为 24 小时
    public static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;
}
