package com.ai.exam_spring.common.exception;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final String code;

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    // 也支持动态消息，比如"题目XXX不存在"
    public BusinessException(ErrorCodeEnum errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
    }
}
