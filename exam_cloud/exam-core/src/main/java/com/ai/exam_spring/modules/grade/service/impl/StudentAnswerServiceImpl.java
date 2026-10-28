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
import com.ai.exam_spring.modules.grade.service.IStudentAnswerService;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
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
    private IPaperManagementService paperManagementService;

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

//    /**
//     * 批量保存学生答案
//     *
//     * @param studentUpdateAnswerDTO
//     */
//    @Override
//    public List<Integer> batchSaveStudentAnswers(StudentUpdateAnswerDTO studentUpdateAnswerDTO) {
//        String currentUserId = SecurityUtil.getCurrentUserId();
//        if (currentUserId == null) {
//            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
//        }
//        PaperContentVO paperContentVO = paperManagementService.getPaperById(studentUpdateAnswerDTO.getPaperId());
//        // 获取该试卷的的题目列表(包含答案)
//        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
//        if (paperQuestionVOS == null || paperQuestionVOS.isEmpty()) {
//            throw new BusinessException(ErrorCodeEnum.PAPER_CONTENT_EMPTY_NOT_GRADE);
//        }
//        // 将题目列表转换为Map，方便后续根据题序快速查找题目
//        Map<Integer, PaperQuestionVO> paperQuestionVOHashMap = paperQuestionVOS
//                .stream()
//                .collect(Collectors.toMap(PaperQuestionVO::getSortOrder, paperQuestionVO -> paperQuestionVO));
//        Integer examId = studentUpdateAnswerDTO.getExamId();
//        List<AnswerDTO> answers = studentUpdateAnswerDTO.getAnswers();
//        // 遍历学生答案列表，封装到StudentAnswer对象中,实现批量插入
//        List<StudentAnswer> studentAnswers = answers.stream().map(answerDTO -> {
//            StudentAnswer studentAnswerObject = new StudentAnswer();
//            PaperQuestionVO paperQuestionVO = paperQuestionVOHashMap.get(answerDTO.getSortOrder());
//            studentAnswerObject.setExamId(examId);
//            studentAnswerObject.setStudentId(Integer.parseInt(currentUserId));
//            studentAnswerObject.setQuestionId(answerDTO.getQuestionId());
//            studentAnswerObject.setFullScore(paperQuestionVO.getScore());
//            return studentAnswerObject;
//        }).toList();
//       transactionTemplate.execute(status -> {
//           baseMapper.insert(studentAnswers);
//           return null;
//        });
//
//        return studentAnswers.stream().map(StudentAnswer::getId).toList();
//    }
}
