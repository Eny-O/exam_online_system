package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.dto.QuestionCompleteDTO;
import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.vo.QuestionCompleteVO;
import com.ai.exam_spring.domain.vo.QuestionVO;

import java.util.List;

/**
 * 题目管理服务接口
 */
public interface QuestionManagementService {
    /**
     * 条件分页查询题目列表
     */
    PageResult<QuestionVO> conditionPageQuery(QuestionQueryDTO queryDTO);

    /**
     * 添加完整题目
     */
    Integer addQuestionComplete(QuestionCompleteDTO questionCompleteDTO);

    /**
     * 根据题目ID查询完整题目
     */
    QuestionCompleteVO getQuestionComplete(Integer id);

    /**
     * 批量删除完整题目
     */
    void removeQuestionCompleteByIds(List<Integer> ids);

    /**
     * 批量查询完整题目
     */
    List<QuestionCompleteVO> getQuestionCompleteByIds(List<Integer> ids);
}
