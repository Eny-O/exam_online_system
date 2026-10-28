package com.ai.exam_auth_user.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    ENABLED(1, "启用"),
    DISABLED(2, "禁用");

    @JsonValue
    @EnumValue
    private final int code;
    private final String description;
}
