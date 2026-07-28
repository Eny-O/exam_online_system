package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.dto.QuestionCompleteDTO;
import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.entity.Question;
import com.ai.exam_spring.domain.vo.QuestionCompleteVO;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 题目管理服务接口
 */
public interface IQuestionManagerService extends IService<Question> {
    /**
     * 条件分页查询题目列表
     */
    PageResult<QuestionVO> conditionPageQuery(QuestionQueryDTO queryDTO);

    /**
     * 添加完整题目
     */
    void addQuestionComplete(QuestionCompleteDTO questionCompleteDTO);

    QuestionCompleteVO getQuestionComplete(Integer id);

    void removeQuestionCompleteByIds(List<Integer> ids);
}
