package com.ai.exam_spring.service.exam.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.mapper.StuExamMapper;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.entity.exam.ExamStudent;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import com.ai.exam_spring.pojo.vo.exam.ExamDetailVO;
import com.ai.exam_spring.pojo.vo.exam.ExamResultVO;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.BasePaperVO;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperQuestionVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.pojo.vo.question.StuQuestionCompleteVO;
import com.ai.exam_spring.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.pojo.vo.stu_answer.ExamAnswerVO;
import com.ai.exam_spring.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.IExamStudentService;
import com.ai.exam_spring.service.exam.StuExamService;
import com.ai.exam_spring.service.grading.GradePaperService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StuExamServiceImpl implements StuExamService {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private GradePaperService gradePaperService;

    @Autowired
    private StuExamMapper stuExamMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
        PaperContentVO paperContentVO = paperManagementService.getPaperById(id);
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
     * 先更新了学生答案，再自动批改了客观题，最后更新了学生的考试状态
     *
     * @param studentUpdateAnswerDTOS
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void submitPaper(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS) {
        if (studentUpdateAnswerDTOS == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Integer currentUserId = getCurrentUserId();
        updateBatchAnswer(studentUpdateAnswerDTOS);
        Integer id = studentUpdateAnswerDTOS.get(0).getId();
        StudentAnswer studentAnswer = studentAnswerService.getById(id);
        Integer examId = studentAnswer.getExamId();
        gradePaperService.autoGradeObjectiveQuestions(examId, currentUserId);
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, currentUserId));
        if (dbExamStudent == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        if (!dbExamStudent.getStatus().equals(StudentExamEnum.PENDING)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_NOT_PENDING_NOT_SUBMIT);
        }
        ExamStudent examStudent = new ExamStudent();
        examStudent.setSubmitTime(LocalDateTime.now());
        examStudent.setStatus(StudentExamEnum.SUBMITTED);
        examStudentService.update(examStudent, new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, currentUserId));
    }


    /**
     * 获取当前用户id
     *
     * @return
     */
    private static @NonNull Integer getCurrentUserId() {
        String currentUserId = SecurityUtil.getCurrentUserId();
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
        if (!dbExamStudent.getStatus().equals(StudentExamEnum.GRADED)) {
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
     * 初始化学生考试信息
     *
     * @param examId
     */
    @Override
    public Map<Integer, Integer> initStuExam(Integer examId) {
        // 判断是否已经初始化,已经初始化了则直接返回
        List<StudentAnswer> dbStudentAnswers = studentAnswerService.list(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getExamId, examId)
                .eq(StudentAnswer::getStudentId, getCurrentUserId()));
        if (dbStudentAnswers != null && !dbStudentAnswers.isEmpty()) {
            return dbStudentAnswers.stream().collect(Collectors.toMap(StudentAnswer::getQuestionId, StudentAnswer::getId));
        }
        Exam exam = examManagementService.getById(examId);
        Integer paperId = exam.getPaperId();
        PaperContentVO paperContentVO = paperManagementService.getPaperById(paperId);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        List<StudentAnswer> studentAnswers = paperContentVO.getPaperQuestionVOS().stream().map(paperQuestionVO -> {
            Double score = paperQuestionVO.getScore();
            Integer questionId = paperQuestionVO.getQuestionCompleteVO().getId();
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setExamId(examId);
            studentAnswer.setStudentId(getCurrentUserId());
            studentAnswer.setQuestionId(questionId);
            studentAnswer.setFullScore(score);
            return studentAnswer;
        }).toList();
        transactionTemplate.execute(status -> {
            studentAnswerService.saveBatch(studentAnswers);
            return null;
        });
        // 根据初始化返回的id,构成map -> (questionId->id)的映射
        return studentAnswers.stream().collect(Collectors.toMap(StudentAnswer::getQuestionId, StudentAnswer::getId));
    }

    /**
     * 更新学生答案
     *
     * @param studentUpdateAnswerDTOS
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateBatchAnswer(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS) {
        if (studentUpdateAnswerDTOS == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (studentUpdateAnswerDTOS.isEmpty()) {
            return;
        }
        List<StudentAnswer> studentAnswers = studentUpdateAnswerDTOS.stream().map(studentUpdateAnswerDTO -> {
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setId(studentUpdateAnswerDTO.getId());
            studentAnswer.setStudentAnswer(studentUpdateAnswerDTO.getStudentAnswer());
            return studentAnswer;
        }).toList();
        studentAnswerService.updateBatchById(studentAnswers);
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
