package com.ai.exam_ai.service;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import com.ai.exam_ai.pojo.enums.GradeStatus;
import lombok.Data;

/**
 * 主观题批阅结论
 */
@Data
public class SubjectiveGradeOutcome {

    /** 批阅状态 */
    private GradeStatus status;

    /** 最终得分 */
    private Double finalScore;

    /** 初评总分 */
    private Double preliminaryScore;

    /** 两评分差占满分比例 */
    private Double divergence;

    /** 初评明细 */
    private InitialGradeResult initialResult;

    /** 再评明细 */
    private ReviewGradeResult reviewResult;

    /** 待人工复核原因 */
    private String escalationReason;

    /**
     * 构造弃权结论
     */
    public static SubjectiveGradeOutcome ungradable(InitialGradeResult initial) {
        SubjectiveGradeOutcome outcome = new SubjectiveGradeOutcome();
        outcome.setStatus(GradeStatus.UNGRADABLE);
        outcome.setInitialResult(initial);
        outcome.setEscalationReason("题目或作答信息不足，无法批阅");
        return outcome;
    }

    /**
     * 构造已复核结论
     */
    public static SubjectiveGradeOutcome reviewed(InitialGradeResult initial, ReviewGradeResult review,
                                                  double divergence) {
        SubjectiveGradeOutcome outcome = new SubjectiveGradeOutcome();
        outcome.setStatus(GradeStatus.GRADED);
        outcome.setFinalScore(review.getFinalScore());
        outcome.setPreliminaryScore(initial.getPreliminaryScore());
        outcome.setDivergence(divergence);
        outcome.setInitialResult(initial);
        outcome.setReviewResult(review);
        return outcome;
    }

    /**
     * 构造待人工复核结论
     */
    public static SubjectiveGradeOutcome needHumanReview(InitialGradeResult initial, ReviewGradeResult review,
                                                         double divergence) {
        SubjectiveGradeOutcome outcome = reviewed(initial, review, divergence);
        outcome.setStatus(GradeStatus.NEED_HUMAN_REVIEW);
        outcome.setEscalationReason("两评分差占满分 " + Math.round(divergence * 100) + "%，超过阈值，需人工复核");
        return outcome;
    }

    /**
     * 构造需人工介入结论（模型批阅结果未通过校验）
     */
    public static SubjectiveGradeOutcome manualReview(String reason) {
        SubjectiveGradeOutcome outcome = new SubjectiveGradeOutcome();
        outcome.setStatus(GradeStatus.NEED_HUMAN_REVIEW);
        outcome.setEscalationReason(reason);
        return outcome;
    }
}
