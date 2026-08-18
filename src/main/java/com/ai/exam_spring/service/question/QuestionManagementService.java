package com.ai.exam_spring.service.question;

import com.ai.exam_spring.common.result.BatchInsertResult;
import com.ai.exam_spring.pojo.dto.question.QuestionAddDTO;
import com.ai.exam_spring.pojo.dto.question.QuestionUpdateDTO;
import com.ai.exam_spring.pojo.dto.question.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.pojo.vo.question.QuestionCompleteVO;
import com.ai.exam_spring.pojo.vo.question.QuestionVO;

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
     * 批量添加完整题目
     */
    BatchInsertResult addQuestionComplete(List<QuestionAddDTO> questionAddDTOS);

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

    /**
     * 更新完整题目
     */
    void updateQuestionComplete(QuestionUpdateDTO questionUpdateDTO);
}
