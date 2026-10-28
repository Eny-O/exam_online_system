package com.ai.exam_spring.common.constant;

/**
 * Redis 常量
 */
public class RedisConstant {
    public static final String LOGIN_USER_PREFIX_KEY = "login: ";

    /**
     * 学生答案Redis缓存key前缀
     * 完整key格式: exam:answer:{examId}:{studentId}
     * Hash field: questionId
     * Hash value: RedisAnswerDTO JSON
     */
    public static final String EXAM_ANSWER_PREFIX = "exam:answer:";

    /**
     * 已发布的考试的试卷内容缓存
     * 完整key格式: exam:publish:{examId}
     * Value: PaperContentVO JSON
     */
    public static final String EXAM_PUBLISH_PREFIX = "exam:publish:";
}
