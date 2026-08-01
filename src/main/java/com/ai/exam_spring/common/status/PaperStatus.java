package com.ai.exam_spring.common.status;

import lombok.Getter;

import java.util.List;

@Getter
public enum PaperStatus {

    DRAFT(0,"草稿"),
    PEDING_ENABLE(1,"待启用"),
    ALREADY_ENABLE(2,"已启用");

    private final Integer code;
    private final String message;

    PaperStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public List<PaperStatus> getAllowedTargets() {
        return switch (this) {
            case DRAFT -> List.of(PEDING_ENABLE);
            case PEDING_ENABLE -> List.of(ALREADY_ENABLE, DRAFT);
            case ALREADY_ENABLE -> List.of(PEDING_ENABLE);
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
