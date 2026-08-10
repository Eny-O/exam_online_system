package com.ai.exam_spring.pojo.enums.question;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 选项标签枚举类
 */
@Getter
public enum OptionLabelEnum {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F");

    @JsonValue
    @EnumValue
    private final String code;

    OptionLabelEnum(String code) {
        this.code = code;
    }

    @JsonCreator
    public static OptionLabelEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OptionLabelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_LABEL_ERROR);
    }

    /**
     * 判断枚举类型是否存在
     * @param code
     * @return
     */
    public static boolean exists(String code) {
        for (OptionLabelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

}
