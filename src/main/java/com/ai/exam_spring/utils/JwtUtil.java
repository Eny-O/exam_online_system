package com.ai.exam_spring.utils;

import com.ai.exam_spring.common.constant.JwtConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 **/
@Slf4j
public class JwtUtil {
    
    /**
     * 获取签名密钥对象
     * @return SecretKey 签名密钥
     */
    private static SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(JwtConstant.SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     * @param subject 主题（通常存放用户ID或用户名）
     * @param claims  自定义载荷数据（非敏感的业务数据）
     * @return 生成的 JWT 字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + JwtConstant.EXPIRATION_TIME);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析并验证 JWT Token
     * @param token JWT 字符串
     * @return 解析后的 Claims 对象
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     * @param token JWT 字符串
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取主题（Subject）
     * @param token JWT 字符串
     * @return 主题字符串，解析失败返回 null
     */
    public static String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取指定的自定义声明值
     * @param token JWT 字符串
     * @param key   声明的键名
     * @return 声明的值，不存在或解析失败返回 null
     */
    public static Object getClaim(String token, String key) {
        return parseToken(token).get(key);
    }
}