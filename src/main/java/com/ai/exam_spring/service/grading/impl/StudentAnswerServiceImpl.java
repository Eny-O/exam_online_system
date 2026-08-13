package com.ai.exam_spring.service.grading.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.mapper.StudentAnswerMapper;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerAddDTO;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerUpdateDTO;
import com.ai.exam_spring.pojo.entity.exam.ExamStudent;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.PaperQuestionVO;
import com.ai.exam_spring.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.service.exam.IExamStudentService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.NonNull;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentAnswerServiceImpl extends ServiceImpl<StudentAnswerMapper, StudentAnswer> implements IStudentAnswerService {

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private IExamStudentService examStudentService;


    /**
     * 根据学生id和考试id获取学生答案
     *
     * @param studentId
     * @param examId
     * @return
     */
    @Override
    public List<StudentAnswerVO> getStudentAnswer(Integer studentId, Integer examId) {
        if (studentId == null || examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<StudentAnswer> studentAnswers = this.list(
                new LambdaQueryWrapper<StudentAnswer>()
                        .eq(StudentAnswer::getStudentId, studentId)
                        .eq(StudentAnswer::getExamId, examId));
        if (studentAnswers == null || studentAnswers.isEmpty()) {
            return null;
        }
        return studentAnswers.stream().map(studentAnswer -> {
            StudentAnswerVO studentAnswerVO = new StudentAnswerVO();
            studentAnswerVO.setStudentAnswer(studentAnswer.getStudentAnswer());
            studentAnswerVO.setIsCorrect(studentAnswer.getIsCorrect());
            studentAnswerVO.setQuestionId(studentAnswer.getQuestionId());
            studentAnswerVO.setScore(studentAnswer.getScore());
            studentAnswerVO.setId(studentAnswer.getId());
            return studentAnswerVO;
        }).toList();
    }

    /**
     * 更新学生得分并且保存分数
     *
     * @param studentAnswerUpdateDTOS
     */
    @Override
    public void updateScoreAndGrade(List<StudentAnswerUpdateDTO> studentAnswerUpdateDTOS) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        if (studentAnswerUpdateDTOS == null || studentAnswerUpdateDTOS.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Integer examId = studentAnswerUpdateDTOS.get(0).getExamId();
        Integer studentId = studentAnswerUpdateDTOS.get(0).getStudentId();
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, studentId));
        if (dbExamStudent == null){
            throw new BusinessException(ErrorCodeEnum.EXAM_STUDENT_NOT_FOUND);
        }
        ExamStudent examStudent = getExamStudent(studentAnswerUpdateDTOS, dbExamStudent);
        List<StudentAnswer> studentAnswers = studentAnswerUpdateDTOS.stream().map(studentAnswerUpdateDTO -> {
            StudentAnswer studentAnswer = new StudentAnswer();
            BeanUtil.copyProperties(studentAnswerUpdateDTO, studentAnswer);
            studentAnswer.setGraderId(Integer.parseInt(currentUserId));
            studentAnswer.setGradeTime(LocalDateTime.now());
            return studentAnswer;
        }).toList();
        // 批量更新学生得分和更新考试学生关联表
        transactionTemplate.execute(status -> {
            this.updateBatchById(studentAnswers);

            examStudentService.update(examStudent, new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStudentId, studentId));
            return null;
        });
    }

    /**
     * 根据学生答案列表和数据库中的学生考试详情，计算并更新考试学生关联信息,返回新的考试学生关联信息
     *
     * @param studentAnswerUpdateDTOS
     * @param dbExamStudent
     * @return ExamStudent
     */
    private static @NonNull ExamStudent getExamStudent(List<StudentAnswerUpdateDTO> studentAnswerUpdateDTOS, ExamStudent dbExamStudent) {
        if (dbExamStudent.getStatus() != StudentExamEnum.SUBMITTED) {
            throw new BusinessException(ErrorCodeEnum.PAPER_STATUS_ERROR_NOT_GRADE);
        }
        // 遍历学生答案列表，计算主观题和客观题的总分
        Double subjectiveScore = 0.0;
        Double objectiveScore = 0.0;
        for (StudentAnswerUpdateDTO studentAnswerUpdateDTO : studentAnswerUpdateDTOS) {
            if (studentAnswerUpdateDTO.getIsCorrect() != null
                    && studentAnswerUpdateDTO.getIsCorrect() == 1
                    && studentAnswerUpdateDTO.getType() != QuestionTypeEnum.ESSAY) {
                subjectiveScore += studentAnswerUpdateDTO.getScore();
            } else if (studentAnswerUpdateDTO.getIsCorrect() != null
                    && studentAnswerUpdateDTO.getIsCorrect() == 1) {
                objectiveScore += studentAnswerUpdateDTO.getScore();
            }
        }
        ExamStudent examStudent = new ExamStudent();
        examStudent.setSubjectiveScore(subjectiveScore);
        examStudent.setObjectiveScore(objectiveScore);
        examStudent.setTotalScore(subjectiveScore + objectiveScore);
        examStudent.setStatus(StudentExamEnum.GRADED);
        return examStudent;
    }

    /**
     * 批量保存学生答案
     *
     * @param studentAnswerAddDTO
     */
    @Override
    public Boolean batchSaveStudentAnswers(StudentAnswerAddDTO studentAnswerAddDTO) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        PaperContentVO paperContentVO = paperManagementService.getPaperById(studentAnswerAddDTO.getPaperId());
        // 获取该试卷的的题目列表(包含答案)
        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
        if (paperQuestionVOS == null || paperQuestionVOS.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PAPER_CONTENT_EMPTY_NOT_GRADE);
        }
        // 将题目列表转换为Map，方便后续根据题序快速查找题目
        Map<Integer, PaperQuestionVO> paperQuestionVOHashMap = paperQuestionVOS
                .stream()
                .collect(Collectors.toMap(PaperQuestionVO::getSortOrder, paperQuestionVO -> paperQuestionVO));
        Integer examId = studentAnswerAddDTO.getExamId();
        // 遍历学生答案列表，封装到StudentAnswer对象中,实现批量插入
        List<StudentAnswer> studentAnswers = studentAnswerAddDTO.getAnswers().stream().map(answerDTO -> {
            StudentAnswer studentAnswerObject = new StudentAnswer();
            PaperQuestionVO paperQuestionVO = paperQuestionVOHashMap.get(answerDTO.getSortOrder());
            String answer = paperQuestionVO.getQuestionCompleteVO().getAnswer();
            String studentAnswer = answerDTO.getStudentAnswer();
            studentAnswerObject.setExamId(examId);
            studentAnswerObject.setStudentId(Integer.parseInt(currentUserId));
            studentAnswerObject.setQuestionId(answerDTO.getQuestionId());
            studentAnswerObject.setStudentAnswer(studentAnswer);
            // 非简答题直接批阅
            if (paperQuestionVO.getQuestionCompleteVO().getType() != QuestionTypeEnum.ESSAY) {
                if (answer.equals(studentAnswer)) {
                    studentAnswerObject.setIsCorrect(1);
                    studentAnswerObject.setScore(paperQuestionVO.getScore());
                } else {
                    studentAnswerObject.setIsCorrect(0);
                    studentAnswerObject.setScore(0.0);
                }
                studentAnswerObject.setFullScore(paperQuestionVO.getScore());
            } else {
                studentAnswerObject.setFullScore(paperQuestionVO.getScore());
            }
            return studentAnswerObject;
        }).toList();
        Boolean isInsertSuccess = false;
        isInsertSuccess = transactionTemplate.execute(status -> {
            baseMapper.insert(studentAnswers);
            return true;
        });
        return isInsertSuccess;
    }
}
