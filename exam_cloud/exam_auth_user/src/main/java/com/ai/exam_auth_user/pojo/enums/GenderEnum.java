package com.ai.exam_auth_user.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
public enum GenderEnum {

    @Schema(description = "未知")
    UNKNOWN(0),
    @Schema(description = "男")
    MALE(1),
    @Schema(description = "女")
    FEMALE(2);

    @JsonValue
    @EnumValue
    private final int code;

    GenderEnum(int code) {
        this.code = code;
    }
}
