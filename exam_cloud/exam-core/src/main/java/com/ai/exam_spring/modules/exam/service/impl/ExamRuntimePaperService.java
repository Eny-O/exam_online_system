package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.mapper.ExamManagementMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperQuestionVO;
import com.ai.exam_spring.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 考试进行期的卷面读取服务：优先读发布缓存，未命中读试卷快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamRuntimePaperService {

    /** 考试缓存的额外保留时长(秒)：考试结束后再保留一天 */
    public static final long CACHE_EXPIRE_AFTER_EXAM_END_SECONDS = 24 * 60 * 60L;

    /** 考试结束后仍允许暂存答案的宽限秒数 */
    private static final long EXAM_END_GRACE_SECONDS = 60L;

    private final RedisUtil redisUtil;
    private final IExamPaperSnapshotService examPaperSnapshotService;
    private final ExamManagementMapper examManagementMapper;

    /**
     * 获取考试进行期的卷面数据
     */
    public StuExamPaperVO getRuntimePaper(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        String key = RedisConstant.EXAM_PUBLISH_PREFIX + examId;
        boolean cacheWritable = true;
        try {
            StuExamPaperVO cached = redisUtil.get(key, StuExamPaperVO.class);
            if (cached != null && cached.getPaper() != null && cached.getExam() != null) {
                return cached;
            }
            if (cached != null) {
                deleteQuietly(key);
            }
        } catch (RuntimeException e) {
            log.warn("考试卷面缓存读取失败 examId={}", examId, e);
            cacheWritable = false;
            deleteQuietly(key);
        }
        StuExamPaperVO paper = snapshotPaper(examId);
        if (cacheWritable) {
            fillCacheQuietly(examId, paper);
        }
        return paper;
    }

    /**
     * 恢复考试卷面缓存
     */
    public void restoreCache(Integer examId) {
        if (examId == null) {
            return;
        }
        try {
            fillCacheQuietly(examId, snapshotPaper(examId));
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("考试卷面缓存恢复失败 examId={}", examId, e);
        }
    }

    /**
     * 校验考试是否处于可作答时间窗内，并返回卷面数据
     */
    public StuExamPaperVO requireWithinExamWindow(Integer examId) {
        StuExamPaperVO paper = getRuntimePaper(examId);
        StuExamVO exam = paper == null ? null : paper.getExam();
        checkExamWindow(examId,
                exam == null ? null : exam.getStartTime(),
                exam == null ? null : exam.getDuration(), true);
        return paper;
    }

    /**
     * 校验考试是否处于可作答时间窗内
     */
    public void assertWithinExamWindow(Integer examId) {
        Exam exam = examOf(examId);
        checkExamWindow(examId, exam.getStartTime(), exam.getDuration(), true);
    }

    /**
     * 校验考试是否已开考
     */
    public void assertExamStarted(Integer examId) {
        Exam exam = examOf(examId);
        checkExamWindow(examId, exam.getStartTime(), exam.getDuration(), false);
    }

    /**
     * 获取时间窗校验所需的考试信息
     */
    private Exam examOf(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Exam exam = examManagementMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        return exam;
    }

    /**
     * 校验考试时间窗
     */
    private static void checkExamWindow(Integer examId, LocalDateTime startTime,
                                        Integer duration, boolean checkEnd) {
        if (startTime == null || duration == null) {
            log.warn("考试缺少开考时间或时长，跳过时间窗校验 examId={}", examId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_STARTED);
        }
        if (!checkEnd) {
            return;
        }
        LocalDateTime deadline = startTime
                .plusMinutes(duration)
                .plusSeconds(EXAM_END_GRACE_SECONDS);
        if (now.isAfter(deadline)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_ALREADY_ENDED);
        }
    }

    /**
     * 计算考试缓存的过期秒数
     */
    public static long cacheExpireSeconds(StuExamVO exam) {
        long expireSeconds = CACHE_EXPIRE_AFTER_EXAM_END_SECONDS;
        if (exam != null && exam.getStartTime() != null && exam.getDuration() != null) {
            LocalDateTime examEndTime = exam.getStartTime().plusMinutes(exam.getDuration());
            long secondsUntilExamEnd = Duration.between(LocalDateTime.now(), examEndTime).getSeconds();
            expireSeconds += Math.max(secondsUntilExamEnd, 0);
        }
        return expireSeconds;
    }

    /**
     * 获取题目ID与分值的映射
     */
    public Map<Integer, Double> getScoreMap(StuExamPaperVO paper) {
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (StuPaperQuestionVO question : questionsOf(paper)) {
            if (question.getStuQuestionCompleteVO() == null
                    || question.getStuQuestionCompleteVO().getId() == null
                    || question.getScore() == null) {
                continue;
            }
            scoreMap.put(question.getStuQuestionCompleteVO().getId(), question.getScore());
        }
        return scoreMap;
    }

    /**
     * 获取题目ID与题型的映射
     */
    public Map<Integer, String> getQuestionTypeMap(Integer examId) {
        Map<Integer, String> typeMap = new HashMap<>();
        for (StuPaperQuestionVO question : questionsOf(examId)) {
            if (question.getStuQuestionCompleteVO() == null
                    || question.getStuQuestionCompleteVO().getId() == null
                    || question.getStuQuestionCompleteVO().getType() == null) {
                continue;
            }
            typeMap.put(question.getStuQuestionCompleteVO().getId(),
                    question.getStuQuestionCompleteVO().getType());
        }
        return typeMap;
    }

    /**
     * 获取卷面题目列表
     */
    private List<StuPaperQuestionVO> questionsOf(Integer examId) {
        return questionsOf(getRuntimePaper(examId));
    }

    private List<StuPaperQuestionVO> questionsOf(StuExamPaperVO paper) {
        if (paper == null || paper.getPaper() == null
                || paper.getPaper().getStuPaperQuestionVOS() == null) {
            return List.of();
        }
        return paper.getPaper().getStuPaperQuestionVOS().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private StuExamPaperVO snapshotPaper(Integer examId) {
        return examPaperSnapshotService.toStuExamPaper(examPaperSnapshotService.getRequired(examId));
    }

    private void fillCacheQuietly(Integer examId, StuExamPaperVO paper) {
        if (paper == null) {
            return;
        }
        try {
            redisUtil.setIfAbsent(RedisConstant.EXAM_PUBLISH_PREFIX + examId, paper,
                    cacheExpireSeconds(paper.getExam()));
        } catch (RuntimeException e) {
            log.warn("考试卷面缓存回填失败 examId={}", examId, e);
        }
    }

    private void deleteQuietly(String key) {
        try {
            redisUtil.delete(key);
        } catch (RuntimeException e) {
            log.warn("考试卷面缓存清理失败 key={}", key, e);
        }
    }
}
