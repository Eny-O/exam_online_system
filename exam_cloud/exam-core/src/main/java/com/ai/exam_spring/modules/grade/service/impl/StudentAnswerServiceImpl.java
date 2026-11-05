package com.ai.exam_spring.modules.grade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.security.SecurityUtil;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.StudentAnswerUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.grade.pojo.vo.StudentScoreSummaryVO;
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentAnswerServiceImpl extends ServiceImpl<StudentAnswerMapper, StudentAnswer> implements IStudentAnswerService {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private SecurityUtil securityUtil;


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
            return List.of();
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

    @Override
    public List<StudentAnswer> listByExamAndStudent(Integer examId, Integer studentId) {
        return this.list(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getExamId, examId)
                .eq(StudentAnswer::getStudentId, studentId));
    }

    /**
     * 更新学生得分并且保存分数
     *
     * @param studentAnswerUpdateDTOS
     */
    @Override
    public void updateScoreAndGrade(List<StudentAnswerUpdateDTO> studentAnswerUpdateDTOS) {
        String currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        if (studentAnswerUpdateDTOS == null || studentAnswerUpdateDTOS.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Integer examId = studentAnswerUpdateDTOS.get(0).getExamId();
        Integer studentId = studentAnswerUpdateDTOS.get(0).getStudentId();
        if (examId == null || studentId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        ExamStudent dbExamStudent = examStudentService.getOne(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, studentId));
        if (dbExamStudent == null){
            throw new BusinessException(ErrorCodeEnum.EXAM_STUDENT_NOT_FOUND);
        }
        ExamStudent examStudent = getExamStudent(dbExamStudent);
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

            List<StudentScoreSummaryVO> summaries =
                    baseMapper.selectScoreSummary(examId, List.of(studentId));
            StudentScoreSummaryVO summary = summaries == null || summaries.isEmpty()
                    ? null : summaries.get(0);
            examStudent.setObjectiveScore(summary == null || summary.getObjectiveScore() == null
                    ? 0.0 : summary.getObjectiveScore());
            examStudent.setSubjectiveScore(summary == null || summary.getSubjectiveScore() == null
                    ? 0.0 : summary.getSubjectiveScore());
            examStudent.setTotalScore(summary == null || summary.getTotalScore() == null
                    ? 0.0 : summary.getTotalScore());

            examStudentService.update(examStudent, new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStudentId, studentId));
            return null;
        });
    }

    /**
     * 根据数据库中的学生考试详情，构建待更新的考试学生关联信息
     *
     * @param dbExamStudent
     * @return ExamStudent
     */
    private static @NonNull ExamStudent getExamStudent(ExamStudent dbExamStudent) {
        if (dbExamStudent.getStatus() != StudentExamEnum.SUBMITTED) {
            throw new BusinessException(ErrorCodeEnum.PAPER_STATUS_ERROR_NOT_GRADE);
        }
        ExamStudent examStudent = new ExamStudent();
        examStudent.setStatus(StudentExamEnum.GRADED);
        return examStudent;
    }

}
