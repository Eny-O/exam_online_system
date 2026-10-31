package com.ai.exam_ai.pojo.enums;

/**
 * 批阅结果状态
 */
public enum GradeStatus {
    /** 已完成批阅 */
    GRADED,
    /** 信息不足，无法批阅（弃权） */
    UNGRADABLE,
    /** 两评分歧超阈值，待人工复核 */
    NEED_HUMAN_REVIEW
}