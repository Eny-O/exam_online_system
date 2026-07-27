package com.ai.exam_spring.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    ENABLED(1, "启用"),
    DISABLED(2, "禁用");

    private final int code;
    private final String description;
}
