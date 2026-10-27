package com.ai.exam_spring.common.status;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.List;

@Getter
public enum PaperStatus {

    DRAFT("DRAFT","草稿"),
    PENDING_ENABLE("PENDING_ENABLE","待启用"),
    ALREADY_ENABLE("ALREADY_ENABLE","已启用");

    @EnumValue
    @JsonValue
    private final String code;
    private final String message;

    PaperStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public List<PaperStatus> getAllowedTargets() {
        return switch (this) {
            case DRAFT -> List.of(PENDING_ENABLE);
            case PENDING_ENABLE -> List.of(ALREADY_ENABLE, DRAFT);
            case ALREADY_ENABLE -> List.of(PENDING_ENABLE);
            default -> throw new IllegalStateException("未知状态 " + this);
        };
    }

    /**
     * 校验是否允许流转到目标状态
     */
    public boolean canTransferTo(PaperStatus target) {
        return getAllowedTargets().contains(target);
    }


}
