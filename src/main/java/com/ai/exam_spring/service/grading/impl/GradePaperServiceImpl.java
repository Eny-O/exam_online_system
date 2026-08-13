package com.ai.exam_spring.service.grading.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerAddDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.entity.user.User;
import com.ai.exam_spring.pojo.vo.grade.GradePaperContentVO;
import com.ai.exam_spring.pojo.vo.grade.GradePaperQuestionVO;
import com.ai.exam_spring.pojo.vo.grade.GradeStudentVO;
import com.ai.exam_spring.pojo.vo.paper.*;
import com.ai.exam_spring.pojo.vo.question.QuestionCompleteVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.grading.GradePaperService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import com.ai.exam_spring.service.user.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradePaperServiceImpl implements GradePaperService {

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Autowired
    private UserService userService;

    /**
     * 根据考试id获取试卷内容
     * 复用试卷管理获取试卷的接口,获得试卷内容封装成批阅试卷内容VO
     *
     * @param examId
     * @return
     */
    @Override
    public GradePaperContentVO getGradePaper(Integer examId) {
        Exam exam = examManagementService.getById(examId);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        Integer paperId = exam.getPaperId();
        if (paperId == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        PaperContentVO paperContentVO = paperManagementService.getPaperById(paperId);
        GradePaperContentVO gradePaperContentVO = new GradePaperContentVO();
        BeanUtil.copyProperties(paperContentVO, gradePaperContentVO);
        // 封装批阅试卷题目列表
        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
        if (paperQuestionVOS == null || paperQuestionVOS.isEmpty()) {
            return gradePaperContentVO;
        }
        List<GradePaperQuestionVO> gradePaperQuestionVOS = paperQuestionVOS.stream().map(paperQuestionVO -> {
            GradePaperQuestionVO gradePaperQuestionVO = new GradePaperQuestionVO();
            gradePaperQuestionVO.setSortOrder(paperQuestionVO.getSortOrder());
            gradePaperQuestionVO.setScore(paperQuestionVO.getScore());
            QuestionCompleteVO questionCompleteVO = paperQuestionVO.getQuestionCompleteVO();
            gradePaperQuestionVO.setContent(questionCompleteVO.getContent());
            gradePaperQuestionVO.setId(questionCompleteVO.getId());
            gradePaperQuestionVO.setCorrectAnswer(questionCompleteVO.getAnswer());
            gradePaperQuestionVO.setOptions(questionCompleteVO.getOptions());
            gradePaperQuestionVO.setType(questionCompleteVO.getType().name());
            return gradePaperQuestionVO;
        }).toList();
        gradePaperContentVO.setGradePaperQuestionVOS(gradePaperQuestionVOS);
        return gradePaperContentVO;
    }



    /**
     * 根据考试id获取批阅试卷学生列表
     *
     * @param examId
     * @return
     */
    @Override
    public List<GradeStudentVO> getGradeStudentList(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<StudentAnswer> studentAnswers = studentAnswerService.list(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getExamId, examId));
        List<Integer> studentIds = studentAnswers.stream().map(StudentAnswer::getStudentId).toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userService.listByIds(studentIds);
        if (users.isEmpty()) {
            return List.of();
        }

        return users.stream().map(user -> {
            GradeStudentVO gradeStudentVO = new GradeStudentVO();
            gradeStudentVO.setStudentId(user.getId());
            gradeStudentVO.setStudentName(user.getRealName());
            gradeStudentVO.setStudentNo(user.getStudentNo());
            return gradeStudentVO;
        }).toList();
    }

    /**
     * 保存批阅学生答案
     *
     * @param studentAnswerAddDTO
     */
    @Override
    public void saveGradeStudentAnswer(StudentAnswerAddDTO studentAnswerAddDTO) {

    }
}
