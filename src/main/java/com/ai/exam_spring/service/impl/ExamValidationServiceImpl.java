package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.domain.entity.PaperQuestion;
import com.ai.exam_spring.mapper.PaperQuestionMapper;
import com.ai.exam_spring.service.ExamValidationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamValidationServiceImpl implements ExamValidationService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    /**
     * 判断题目是否被其他试卷引用
     * @param questionIds
     * @return
     */
    @Override
    public boolean validateQuestionsReferenceAnyPaper(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return false;
        return paperQuestionMapper.exists(
                new LambdaQueryWrapper<PaperQuestion>()
                        .in(PaperQuestion::getQuestionId, questionIds));
    }
}
