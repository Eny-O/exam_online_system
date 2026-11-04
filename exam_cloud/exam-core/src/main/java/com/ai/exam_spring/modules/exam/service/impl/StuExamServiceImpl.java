package com.ai.exam_spring.modules.exam.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_spring.modules.exam.mapper.StuExamMapper;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.RedisAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamDetailVO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.paper.pojo.vo.BasePaperVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.pojo.vo.StuQuestionCompleteVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StuAnswerReviewVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.exam.service.StuExamService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import com.ai.exam_spring.modules.paper.service.PaperContentConverter;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StuExamServiceImpl implements StuExamService {

    /** 考生答案缓存的额外保留时长(秒)：考试结束后再保留一天 */
    private static final long ANSWER_CACHE_EXPIRE_AFTER_EXAM_END_SECONDS = 24 * 60 * 60L;

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private PaperContentConverter paperContentConverter;

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
     * 数据来自教师发布考试时写入的redis缓存
     *
     * @param examId 考试ID
     * @return 考试信息与试卷内容
     */
    @Override
    public StuExamPaperVO getExamPaperWithExam(Integer examId) {
        StuExamPaperVO examPaperVO = redisUtil.get(RedisConstant.EXAM_PUBLISH_PREFIX + examId, StuExamPaperVO.class);
        if (examPaperVO == null || examPaperVO.getPaper() == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        return examPaperVO;
    }

    /**
     * 根据试卷ID获取试卷内容
     *
     * @param paperId 试卷ID
     * @return StuPaperVO
     */
    @Override
    public StuPaperVO getPaperById(Integer paperId) {
        PaperContentVO paperContentVO = paperManagementService.getPaperById(paperId);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        return paperContentConverter.toStudentPaper(paperContentVO);
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
     * 复用试卷管理的完整题目数据（含答案与解析），与学生本人的作答合并返回
     *
     * @param examId 考试ID
     * @return 题目（含标准答案与解析）+ 学生作答合并列表
     */
    @Override
    public List<StuAnswerReviewVO> getAnswerReview(Integer examId) {
        Integer currentUserId = getCurrentUserId();
        Exam exam = examManagementService.getById(examId);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        Integer paperId = exam.getPaperId();
        if (paperId == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        PaperContentVO paperContentVO = paperManagementService.getPaperById(paperId);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
        if (paperQuestionVOS == null || paperQuestionVOS.isEmpty()) {
            return List.of();
        }
        List<StudentAnswerVO> studentAnswerVOS = studentAnswerService.getStudentAnswer(currentUserId, examId);
        Map<Integer, StudentAnswerVO> answerMap = studentAnswerVOS.stream()
                .collect(Collectors.toMap(StudentAnswerVO::getQuestionId, Function.identity(), (a, b) -> a));

        return paperQuestionVOS.stream().map(paperQuestionVO -> {
            QuestionCompleteVO questionCompleteVO = paperQuestionVO.getQuestionCompleteVO();
            StuAnswerReviewVO vo = new StuAnswerReviewVO();
            if (questionCompleteVO == null) {
                return vo;
            }
            vo.setQuestionId(questionCompleteVO.getId());
            vo.setType(questionCompleteVO.getType() == null ? null : questionCompleteVO.getType().name());
            vo.setContent(questionCompleteVO.getContent());
            vo.setOptions(questionCompleteVO.getOptions());
            vo.setCorrectAnswer(questionCompleteVO.getAnswer());
            vo.setExplanation(questionCompleteVO.getAnalysis());
            vo.setFullScore(paperQuestionVO.getScore());
            StudentAnswerVO answer = answerMap.get(questionCompleteVO.getId());
            if (answer != null) {
                vo.setStudentAnswer(answer.getStudentAnswer());
                vo.setIsCorrect(answer.getIsCorrect());
                vo.setScore(answer.getScore());
                vo.setTeacherComment(answer.getTeacherComment());
            }
            return vo;
        }).toList();
    }

    /**
     * 获取考试结果详情
     *
     * @param examId
     * @return
     */
    @Override
    public ExamResultVO getExamResult(Integer examId) {
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, getCurrentUserId()));
        if (dbExamStudent == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        if (!StudentExamEnum.GRADED.equals(dbExamStudent.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_GRADED);
        }
        ExamDetailVO dbExamDetail = examManagementService.getExamDetailById(examId);
        return getExamResultVO(dbExamDetail, dbExamStudent);
    }

    /**
     * 获取所有考试结果
     *
     * @return
     */
    @Override
    public List<ExamResultVO> getAllExamResult() {
        return stuExamMapper.getAllExamResult(getCurrentUserId());
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

        Map<Integer, Exam> examCache = new HashMap<>();
        Map<Integer, Map<Integer, Double>> scoreCache = new HashMap<>();
        Map<Integer, Long> ttlCache = new HashMap<>();
        for (StudentUpdateAnswerDTO dto : studentUpdateAnswerDTOS) {
            Integer examId = dto.getExamId();
            Integer questionId = dto.getQuestionId();
            if (examId == null || questionId == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            Map<Integer, Double> scoreMap = scoreCache.computeIfAbsent(examId, id -> {
                Exam exam = examCache.computeIfAbsent(id, examManagementService::getById);
                return paperManagementService.getQuestionScoreMap(requirePaperId(exam));
            });
            Double fullScore = scoreMap.get(questionId);
            if (fullScore == null) {
                throw new BusinessException(ErrorCodeEnum.EXAM_QUESTION_NOT_IN_PAPER);
            }

            String redisKey = RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + currentUserId;
            String field = String.valueOf(questionId);

            // 先读取已有的答案数据（保留createTime）
            RedisAnswerDTO existing = redisUtil.hGet(redisKey, field, RedisAnswerDTO.class);
            RedisAnswerDTO answerDTO = new RedisAnswerDTO();
            answerDTO.setStudentAnswer(dto.getStudentAnswer());
            answerDTO.setFullScore(fullScore);
            answerDTO.setCreateTime(existing != null ? existing.getCreateTime() : now);
            answerDTO.setUpdateTime(now);

            Map<String, Object> map = new HashMap<>();
            map.put(field, answerDTO);
            long ttlSeconds = ttlCache.computeIfAbsent(examId, id -> {
                Exam exam = examCache.computeIfAbsent(id, examManagementService::getById);
                return answerCacheExpireSeconds(exam);
            });
            redisUtil.hSetAll(redisKey, map, ttlSeconds);
        }
    }

    /**
     * 获取考试关联的试卷ID
     *
     * @param exam 考试
     * @return 试卷ID
     */
    private static Integer requirePaperId(Exam exam) {
        if (exam == null || exam.getPaperId() == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_REFERENCE_PAPER_NOT_FOUND);
        }
        return exam.getPaperId();
    }

    /**
     * 计算考生答案缓存的过期时间
     *
     * @param exam 考试
     * @return 过期秒数
     */
    private static long answerCacheExpireSeconds(Exam exam) {
        long expireSeconds = ANSWER_CACHE_EXPIRE_AFTER_EXAM_END_SECONDS;
        if (exam != null && exam.getStartTime() != null && exam.getDuration() != null) {
            LocalDateTime examEndTime = exam.getStartTime().plusMinutes(exam.getDuration());
            long secondsUntilExamEnd = Duration.between(LocalDateTime.now(), examEndTime).getSeconds();
            expireSeconds += Math.max(secondsUntilExamEnd, 0);
        }
        return expireSeconds;
    }

    /**
     * 组装考试结果
     *
     * @param dbExamDetail  考试详情
     * @param dbExamStudent 考试学生信息
     * @return 考试结果VO
     */
    private static @NonNull ExamResultVO getExamResultVO(ExamDetailVO dbExamDetail, ExamStudent dbExamStudent) {
        BasePaperVO basePaperVO = dbExamDetail.getBasePaperVO();
        if (basePaperVO == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_REFERENCE_PAPER_NOT_FOUND);
        }
        ExamResultVO examResultVO = new ExamResultVO();
        examResultVO.setExamName(dbExamDetail.getExamName());
        examResultVO.setExamId(dbExamDetail.getId());
        examResultVO.setExamId(dbExamDetail.getId());
        examResultVO.setTotalScore(basePaperVO.getTotalScore());
        examResultVO.setExamTime(dbExamDetail.getStartTime());
        examResultVO.setSubmitTime(dbExamStudent.getSubmitTime());
        examResultVO.setExamScore(dbExamStudent.getTotalScore());
        examResultVO.setExamDuration(dbExamDetail.getDuration());
        return examResultVO;
    }

}