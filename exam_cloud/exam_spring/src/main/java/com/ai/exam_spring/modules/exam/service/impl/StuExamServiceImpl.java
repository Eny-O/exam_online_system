package com.ai.exam_spring.modules.exam.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.constant.RedisConstant;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.mapper.StuExamMapper;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
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
import com.ai.exam_spring.modules.paper.pojo.vo.BasePaperVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.question.pojo.vo.StuQuestionCompleteVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.exam.service.StuExamService;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.utils.RedisUtil;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StuExamServiceImpl implements StuExamService {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private StuExamMapper stuExamMapper;

    @Autowired
    private StudentAnswerMapper studentAnswerMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedisUtil redisUtil;

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
     * 根据id获取试卷信息
     *
     * @param id
     * @return StuPaperVO
     */
    @Override
    public StuPaperVO getPaperById(Integer id) {
        PaperContentVO paperContentVO = redisUtil.get(RedisConstant.EXAM_PUBLISH_PREFIX + id, PaperContentVO.class);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        StuPaperVO stuPaperVO = new StuPaperVO();
        BeanUtil.copyProperties(paperContentVO, stuPaperVO);
        List<StuPaperQuestionVO> stuPaperQuestionVOS = paperContentVO.getPaperQuestionVOS().stream().map(paperQuestionVO -> {
            StuPaperQuestionVO stuPaperQuestionVO = new StuPaperQuestionVO();
            BeanUtil.copyProperties(paperQuestionVO, stuPaperQuestionVO);
            StuQuestionCompleteVO stuQuestionCompleteVO = new StuQuestionCompleteVO();
            BeanUtil.copyProperties(paperQuestionVO.getQuestionCompleteVO(), stuQuestionCompleteVO);
            stuPaperQuestionVO.setStuQuestionCompleteVO(stuQuestionCompleteVO);
            return stuPaperQuestionVO;
        }).toList();
        stuPaperVO.setStuPaperQuestionVOS(stuPaperQuestionVOS);
        return stuPaperVO;
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
        // 从Redis读取暂存答案，批量写入数据库
        String redisKey = RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + currentUserId;
        // 使用轻量级方法，避免50次JSON反序列化开销
        Map<String, String> rawAnswerMap = redisUtil.hGetAllRaw(redisKey);
        if (!rawAnswerMap.isEmpty()) {
            List<StudentAnswer> studentAnswers = new ArrayList<>();
            for (Map.Entry<String, String> entry : rawAnswerMap.entrySet()) {
                Integer questionId = Integer.parseInt(entry.getKey());
                String jsonValue = entry.getValue();
                // 轻量级字符串解析，提取RedisAnswerDTO的所有字段
                String studentAnswer = extractJsonField(jsonValue, "studentAnswer");
                Double fullScore = extractJsonDoubleField(jsonValue, "fullScore");

                StudentAnswer studentAnswerEntity = new StudentAnswer();
                studentAnswerEntity.setExamId(examId);
                studentAnswerEntity.setStudentId(currentUserId);
                studentAnswerEntity.setQuestionId(questionId);
                studentAnswerEntity.setStudentAnswer(studentAnswer);
                studentAnswerEntity.setFullScore(fullScore);
                studentAnswers.add(studentAnswerEntity);
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
                                .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)  // ← 关键：WHERE 里加状态条件
                );
                if (rows == 0) {
                    throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT);
                }
                studentAnswerMapper.batchInsert(studentAnswers);
                return null;
            });
            // 写入DB后删除Redis缓存
            // redisUtil.delete(redisKey);
            // mq -> 自动批阅客观题
            rocketMQTemplate.convertAndSend("exam-grade-topic", new GradeMessageDTO(examId, currentUserId));
        } else {
            throw new BusinessException(ErrorCodeEnum.NOT_SUBMIT_EMPTY_PAPER);
        }

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

        for (StudentUpdateAnswerDTO dto : studentUpdateAnswerDTOS) {
            String redisKey = RedisConstant.EXAM_ANSWER_PREFIX + dto.getExamId() + ":" + currentUserId;
            String field = String.valueOf(dto.getQuestionId());

            // 先读取已有的答案数据（保留createTime）
            RedisAnswerDTO existing = redisUtil.hGet(redisKey, field, RedisAnswerDTO.class);
            RedisAnswerDTO answerDTO = new RedisAnswerDTO();
            answerDTO.setStudentAnswer(dto.getStudentAnswer());
            answerDTO.setFullScore(dto.getFullScore());
            answerDTO.setCreateTime(existing != null ? existing.getCreateTime() : now);
            answerDTO.setUpdateTime(now);

            Map<String, Object> map = new HashMap<>();
            map.put(field, answerDTO);
            redisUtil.hSetAll(redisKey, map);
        }
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

    /**
     * 从JSON字符串中提取字符串字段值（轻量级，避免完整JSON解析）
     * 例如: {"studentAnswer":"A","fullScore":2.0} -> extractJsonField(json, "studentAnswer") -> "A"
     */
    private static String extractJsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) {
            return null;
        }
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            return null;
        }
        return json.substring(start, end);
    }

    /**
     * 从JSON字符串中提取数字字段值（轻量级，避免完整JSON解析）
     * 例如: {"studentAnswer":"A","fullScore":2.0} -> extractJsonDoubleField(json, "fullScore") -> 2.0
     */
    private static Double extractJsonDoubleField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":";
        int start = json.indexOf(key);
        if (start == -1) {
            return null;
        }
        start += key.length();
        // 跳过可能的空格
        while (start < json.length() && json.charAt(start) == ' ') {
            start++;
        }
        // 找到数字的结束位置（逗号、}、空格等）
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ' ' || c == '\n' || c == '\r') {
                break;
            }
            end++;
        }
        if (end > start) {
            try {
                return Double.parseDouble(json.substring(start, end));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
