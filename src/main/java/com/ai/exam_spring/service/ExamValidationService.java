package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.entity.PaperQuestion;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;

/**
 * 打破相互调用检验导致的依赖循环
 * 考试验证服务接口
 */
public interface ExamValidationService {

    boolean validateQuestionsReferenceAnyPaper(List<Integer> questionIds);

}
