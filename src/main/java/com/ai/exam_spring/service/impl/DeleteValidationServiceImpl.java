package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.mapper.ExamManagementMapper;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.entity.paper.PaperQuestion;
import com.ai.exam_spring.mapper.PaperQuestionMapper;
import com.ai.exam_spring.service.DeleteValidationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeleteValidationServiceImpl implements DeleteValidationService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private ExamManagementMapper examManagementMapper;
    /**
     * 判断题目是否被其他试卷引用
     * @param questionIds
     * @return
     */
    @Override
    public boolean validateQuestionsReferenceAnyPaper(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return false;
        return !paperQuestionMapper.exists(
                new LambdaQueryWrapper<PaperQuestion>()
                        .in(PaperQuestion::getQuestionId, questionIds));
    }

    @Override
    public boolean validatePaperReferenceAnyExam(List<Integer> paperIds) {
       if (paperIds == null || paperIds.isEmpty()) return false;
       return !examManagementMapper.exists(
                new LambdaQueryWrapper<Exam>()
                        .in(Exam::getPaperId, paperIds));
    }
}
