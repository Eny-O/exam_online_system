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
    PLEASE_FLUSH_AFTER_RETRY("1007", "请刷新页面后重试"),
    PARAM_ERROR("1008", "参数错误"),

    // 题目错误码
    QUESTION_NOT_FOUND("2001", "题目不存在"),
    QUESTION_REFERENCE_PAPER("2002", "题目关联了试卷"),

    // 考试错误码
    EXAM_NOT_FOUND("3001", "考试不存在"),

    // 文件与导入错误码
    FILE_FORMAT_ERROR("4001", "仅支持 .xlsx 格式文件"),
    FILE_EMPTY("4002", "上传文件为空"),
    FILE_PARSE_ERROR("4003", "文件解析失败，请检查文件内容"),
    FILE_CONTENT_REQUIRED_FIELD_EMPTY("4004", "文件内容有必填项为空"),

    // 试卷错误码
    PAPER_NOT_EMPTY("5001", "试卷不能为空"),
    PAPER_NOT_EXIST("5002","试卷不存在"),
    PAPER_ALREADY_ENABLE_NOT_UPDATE("5003",  "试卷已启用不能更改"),
    PAPER_ALREADY_ENABLE_NOT_DELETE("5004", "试卷已启用不能删除"),
    PAPER_STATUS_TRANSFER_NOT_ALLOWED("5005", "试卷状态转换不允许");

    private final String code;
    private final String message;
}
