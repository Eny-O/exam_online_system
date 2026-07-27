package com.ai.exam_spring.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举类
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {
    // HTTP 标准状态码
    SUCCESS("200", "操作成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "认证失败"),
    FORBIDDEN("403", "权限不足"),
    NOT_FOUND("404", "资源不存在"),
    INTERNAL_ERROR("500", "服务器内部错误"),

    // 业务错误码
    OPERATION_FAILED("1000", "操作失败"),
    USER_NOT_FOUND("1001", "用户不存在"),
    USER_ALREADY_EXISTS("1002", "用户已存在"),
    USER_PASSWORD_ERROR("1003", "密码错误"),
    USER_NOT_LOGIN("1004", "用户未登录"),
    PERMISSION_DENIED("1005", "无权限"),
    ACCOUNT_DISABLED("1006", "账号已被禁用"),
    QUESTION_NOT_FOUND("2001", "题目不存在"),
    EXAM_NOT_FOUND("3001", "考试不存在");

    private final String code;
    private final String message;
}
