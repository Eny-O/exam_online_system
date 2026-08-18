package com.ai.exam_spring.service.common;

import java.util.List;

/**
 * 打破相互调用检验导致的依赖循环
 * 删除校验服务接口
 */
public interface DeleteValidationService {

    /**
     * 校验题库中的题目是否被任何试卷引用
     * @param questionIds 题目ID列表
     * @return 是否被任何试卷引用
     */
    boolean validateQuestionsReferenceAnyPaper(List<Integer> questionIds);

    /**
     * 校验试卷是否被任何考试引用
     * @param paperIds 试卷ID列表
     * @return 是否被任何考试引用
     */
    boolean validatePaperReferenceAnyExam(List<Integer> paperIds);

}
