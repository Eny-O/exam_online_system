package com.ai.exam_ai.service;

import com.ai.exam_ai.ai.GradingContext;
import com.ai.exam_ai.ai.SubjectiveGradingWorkflow;
import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import com.ai.exam_ai.pojo.enums.GradeStatus;
import com.ai.exam_api.feign.StudentAnswerFeignClient;
import com.ai.exam_api.pojo.dto.StudentAnswerDTO;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 主观题双 Agent 批阅编排
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectiveGradingService {

    private final SubjectiveGradingWorkflow gradingWorkflow;
    private final GradingContext gradingContext;
    private final StudentAnswerFeignClient studentAnswerFeignClient;

    /** 分歧阈值：超过该值标记待人工复核 */
    @Value("${exam.ai.grading.divergence-threshold:0.30}")
    private double divergenceThreshold;

    /**
     * 对一道主观题完成初评与再评，给出最终分数
     *
     * @param examId     考试ID
     * @param studentId  学生ID
     * @param questionId 题目ID
     * @param fullScore  该题满分
     */
    public SubjectiveGradeOutcome grade(Integer examId, Integer studentId, Integer questionId, Double fullScore) {
        try {
            ReviewGradeResult review = gradingWorkflow.grade(examId, studentId, questionId);
            InitialGradeResult initial = gradingContext.getInitialGrade();
            if (initial == null || GradeStatus.UNGRADABLE.name().equals(initial.getStatus())) {
                return SubjectiveGradeOutcome.ungradable(initial);
            }
            if (review == null || GradeStatus.UNGRADABLE.name().equals(review.getStatus())) {
                return SubjectiveGradeOutcome.ungradable(initial);
            }

            double divergence = calculateDivergence(initial.getPreliminaryScore(), review.getFinalScore(), fullScore);
            if (divergence > divergenceThreshold) {
                return SubjectiveGradeOutcome.needHumanReview(initial, review, divergence);
            }
            return SubjectiveGradeOutcome.reviewed(initial, review, divergence);
        } finally {
            gradingContext.clear();
        }
    }

    /**
     * 按考试/学生/题目三 ID 定位作答并自动批阅（对外可验证入口）
     *
     * @param examId     考试ID
     * @param studentId  学生ID
     * @param questionId 题目ID
     */
    public SubjectiveGradeOutcome gradeByIds(Integer examId, Integer studentId, Integer questionId) {
        List<StudentAnswerDTO> answers = studentAnswerFeignClient.listByExamAndStudent(examId, studentId);
        StudentAnswerDTO target = answers.stream()
                .filter(a -> questionId.equals(a.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCodeEnum.EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE, "未找到该题目的作答记录，无法自动批阅"));
        if (target.getFullScore() == null) {
            return SubjectiveGradeOutcome.ungradable(null);
        }
        return grade(examId, studentId, questionId, target.getFullScore());
    }

    /**
     * 计算两评分差占满分的比例
     */
    private double calculateDivergence(Double initialScore, Double finalScore, Double fullScore) {
        if (initialScore == null || finalScore == null || fullScore == null || fullScore <= 0) {
            return 1.0;
        }
        return Math.abs(initialScore - finalScore) / fullScore;
    }
}
