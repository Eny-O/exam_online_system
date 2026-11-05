package com.ai.exam_spring.modules.exam.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_spring.modules.exam.mapper.StuExamMapper;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamResultMetaDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.RedisAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StuAnswerReviewVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.exam.service.StuExamService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StuExamServiceImpl implements StuExamService {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private IExamPaperSnapshotService examPaperSnapshotService;

    @Autowired
    private ExamRuntimePaperService examRuntimePaperService;

    @Autowired
    private StuExamMapper stuExamMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ExamAnswerPersistenceService examAnswerPersistenceService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SecurityUtil securityUtil;


    @Override
    public StuExamVO examInfoById(Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Exam exam = examManagementService.getById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        StuExamVO stuExamVO = new StuExamVO();
        BeanUtil.copyProperties(exam, stuExamVO);
        return stuExamVO;
    }

    /**
     * 获取某场考试的考试信息与试卷内容
     *
     * @param examId 考试ID
     * @return 考试信息与试卷内容
     */
    @Override
    public StuExamPaperVO getExamPaperWithExam(Integer examId) {
        examRuntimePaperService.assertWithinExamWindow(examId);
        return examPaperSnapshotService.toStuExamPaper(examPaperSnapshotService.getRequired(examId));
    }

    /**
     * 根据考试ID获取该场考试的试卷快照
     *
     * @param examId 考试ID
     * @return StuPaperVO
     */
    @Override
    public StuPaperVO getPaperById(Integer examId) {
        return examPaperSnapshotService.toStudentPaper(examPaperSnapshotService.getRequired(examId));
    }

    /**
     * 提交试卷
     * 1. 更新考试状态为已提交
     * 2. 从Redis读取暂存答案，批量写入数据库
     * 3. 自动批阅客观题
     *
     * @param examId 考试ID
     */
    @Override
    public void submitPaper(Integer examId) {
        Integer currentUserId = getCurrentUserId();
        examRuntimePaperService.assertExamStarted(examId);
        // 从Redis读取暂存答案并落库（与定时自动交卷共用同一份实现）
        List<StudentAnswer> studentAnswers = examAnswerPersistenceService.loadAnswersFromRedis(examId, currentUserId);
        if (studentAnswers.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.NOT_SUBMIT_EMPTY_PAPER);
        }
        // 更新试卷状态, 多值批量插入
        transactionTemplate.execute(status -> {
            int rows = examStudentService.getBaseMapper().update(
                    new ExamStudent() {{
                        setSubmitTime(LocalDateTime.now());
                        setStatus(StudentExamEnum.SUBMITTED);
                    }},
                    new LambdaQueryWrapper<ExamStudent>()
                            .eq(ExamStudent::getExamId, examId)
                            .eq(ExamStudent::getStudentId, currentUserId)
                            .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)
            );
            if (rows == 0) {
                throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT);
            }
            examAnswerPersistenceService.saveAnswers(studentAnswers);
            return null;
        });
        // mq -> 自动批阅客观题
        rocketMQTemplate.convertAndSend("exam-grade-topic", new GradeMessageDTO(examId, currentUserId));
        // 落库成功后清理 Redis 中的暂存答案
        redisUtil.delete(RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + currentUserId);
    }


    /**
     * 获取当前用户id
     *
     * @return
     */
    private @NonNull Integer getCurrentUserId() {
        String currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        return Integer.parseInt(currentUserId);
    }

    /**
     * 获取试卷答案详情
     *
     * @param examId
     * @return
     */
    @Override
    public List<BaseStudentAnswerVO> getAnswerDetail(Integer examId) {
        List<StudentAnswerVO> studentAnswerVOS = studentAnswerService.getStudentAnswer(getCurrentUserId(), examId);

        return studentAnswerVOS.isEmpty() ? List.of() : studentAnswerVOS.stream().map(studentStudentAnswerVO -> {
            BaseStudentAnswerVO baseStudentAnswerVO = new BaseStudentAnswerVO();
            BeanUtil.copyProperties(studentStudentAnswerVO, baseStudentAnswerVO);
            return baseStudentAnswerVO;
        }).toList();
    }

    /**
     * 获取学生答卷回顾（含标准答案与解析，仅向考生本人返回）
     *
     * @param examId 考试ID
     * @return 题目（含标准答案与解析）+ 学生作答合并列表
     */
    @Override
    public List<StuAnswerReviewVO> getAnswerReview(Integer examId) {
        Integer currentUserId = getCurrentUserId();
        ExamPaperSnapshotDTO snapshot = examPaperSnapshotService.getRequired(examId);
        List<StudentAnswerVO> studentAnswerVOS = studentAnswerService.getStudentAnswer(currentUserId, examId);
        Map<Integer, StudentAnswerVO> answerMap = studentAnswerVOS == null
                ? Map.of()
                : studentAnswerVOS.stream()
                .filter(answer -> answer != null && answer.getQuestionId() != null)
                .collect(Collectors.toMap(StudentAnswerVO::getQuestionId, Function.identity(), (a, b) -> a));
        return examPaperSnapshotService.toAnswerReview(snapshot, answerMap);
    }

    /**
     * 获取考试结果详情
     *
     * @param examId
     * @return
     */
    @Override
    public ExamResultVO getExamResult(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, getCurrentUserId()));
        if (dbExamStudent == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        if (!StudentExamEnum.GRADED.equals(dbExamStudent.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_GRADED);
        }
        Map<Integer, ExamResultMetaDTO> metaMap = examPaperSnapshotService.getResultMetaMap(List.of(examId));
        ExamResultMetaDTO meta = metaMap == null ? null : metaMap.get(examId);
        if (meta == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_SNAPSHOT_NOT_FOUND);
        }
        return getExamResultVO(examId, meta, dbExamStudent);
    }

    /**
     * 获取所有考试结果
     *
     * @return
     */
    @Override
    public List<ExamResultVO> getAllExamResult() {
        List<ExamResultVO> queried = stuExamMapper.getAllExamResult(getCurrentUserId());
        if (queried == null || queried.isEmpty()) {
            return List.of();
        }
        List<ExamResultVO> results = new ArrayList<>(queried);
        results.removeIf(Objects::isNull);
        if (results.isEmpty()) {
            return List.of();
        }
        List<Integer> examIds = results.stream()
                .map(ExamResultVO::getExamId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, ExamResultMetaDTO> metaMap = examIds.isEmpty()
                ? Map.of()
                : examPaperSnapshotService.getResultMetaMap(examIds);
        Map<Integer, ExamResultMetaDTO> safeMetaMap = metaMap == null ? Map.of() : metaMap;
        for (ExamResultVO vo : results) {
            fillExamResultMeta(vo, safeMetaMap.get(vo.getExamId()));
        }
        Comparator<ExamResultVO> byExamTimeDesc = Comparator.comparing(
                ExamResultVO::getExamTime,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));
        results.sort(byExamTimeDesc);
        return results;
    }

    /**
     * 初始化学生考试
     * 仅校验考试和学生信息
     * 答案由学生答题时写入Redis暂存，提交时批量写入数据库
     * @param examId
     */
    @Override
    public void initStuExam(Integer examId) {
        Integer currentUserId = getCurrentUserId();
        examRuntimePaperService.assertWithinExamWindow(examId);
        // 校验考试是否存在
        Exam exam = examManagementService.getById(examId);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        // 校验学生是否已关联该考试
        ExamStudent examStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, currentUserId));
        if (examStudent == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
    }

    /**
     * 更新学生答案（写入Redis暂存）
     * 使用Hash结构: key=exam:answer:{examId}:{studentId}, field=questionId, value=RedisAnswerDTO JSON
     *
     * @param studentUpdateAnswerDTOS
     */
    @Override
    public void updateBatchAnswer(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS) {
        if (studentUpdateAnswerDTOS == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (studentUpdateAnswerDTOS.isEmpty()) {
            return;
        }
        Integer currentUserId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        Map<Integer, StuExamPaperVO> runtimeCache = new HashMap<>();
        Map<Integer, Map<Integer, Double>> scoreCache = new HashMap<>();
        Map<Integer, Long> ttlCache = new HashMap<>();
        for (StudentUpdateAnswerDTO dto : studentUpdateAnswerDTOS) {
            if (dto == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            Integer examId = dto.getExamId();
            Integer questionId = dto.getQuestionId();
            if (examId == null || questionId == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            StuExamPaperVO runtimePaper = runtimeCache.computeIfAbsent(examId,
                    examRuntimePaperService::requireWithinExamWindow);
            Map<Integer, Double> scoreMap = scoreCache.computeIfAbsent(examId,
                    id -> examRuntimePaperService.getScoreMap(runtimePaper));
            Double fullScore = scoreMap.get(questionId);
            if (fullScore == null) {
                throw new BusinessException(ErrorCodeEnum.EXAM_QUESTION_NOT_IN_PAPER);
            }

            String redisKey = RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + currentUserId;
            String field = String.valueOf(questionId);

            RedisAnswerDTO existing = redisUtil.hGet(redisKey, field, RedisAnswerDTO.class);
            RedisAnswerDTO answerDTO = new RedisAnswerDTO();
            answerDTO.setStudentAnswer(dto.getStudentAnswer());
            answerDTO.setFullScore(fullScore);
            answerDTO.setCreateTime(existing != null ? existing.getCreateTime() : now);
            answerDTO.setUpdateTime(now);

            Map<String, Object> map = new HashMap<>();
            map.put(field, answerDTO);
            long ttlSeconds = ttlCache.computeIfAbsent(examId, id ->
                    ExamRuntimePaperService.cacheExpireSeconds(
                            runtimePaper == null ? null : runtimePaper.getExam()));
            redisUtil.hSetAll(redisKey, map, ttlSeconds);
        }
    }

    /**
     * 用快照元数据补齐考试结果
     */
    private static void fillExamResultMeta(ExamResultVO vo, ExamResultMetaDTO meta) {
        if (meta == null) {
            return;
        }
        vo.setExamName(meta.getExamName());
        vo.setExamTime(meta.getStartTime());
        vo.setExamDuration(meta.getDuration());
        vo.setTotalScore(meta.getTotalScore());
    }

    /**
     * 组装考试结果
     */
    private static @NonNull ExamResultVO getExamResultVO(Integer examId, ExamResultMetaDTO meta,
                                                         ExamStudent dbExamStudent) {
        ExamResultVO examResultVO = new ExamResultVO();
        examResultVO.setExamId(examId);
        examResultVO.setSubmitTime(dbExamStudent.getSubmitTime());
        examResultVO.setExamScore(dbExamStudent.getTotalScore());
        examResultVO.setStudentExamStatus(dbExamStudent.getStatus());
        fillExamResultMeta(examResultVO, meta);
        return examResultVO;
    }

}