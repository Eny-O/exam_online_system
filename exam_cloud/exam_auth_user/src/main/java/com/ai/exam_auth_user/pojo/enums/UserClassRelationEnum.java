package com.ai.exam_auth_user.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户-班级关系类型
 */
@Getter
@AllArgsConstructor
public enum UserClassRelationEnum {

    STUDENT(1, "学生归属"),
    TEACHER(2, "教师任教");

    @JsonValue
    @EnumValue
    private final int code;

    private final String description;
}
