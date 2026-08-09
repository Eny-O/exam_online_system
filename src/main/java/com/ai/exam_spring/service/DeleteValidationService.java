package com.ai.exam_spring.service;

import java.util.List;

/**
 * 打破相互调用检验导致的依赖循环
 * 删除校验服务接口
 */
public interface DeleteValidationService {

    boolean validateQuestionsReferenceAnyPaper(List<Integer> questionIds);

    boolean validatePaperReferenceAnyExam(List<Integer> paperIds);

}
