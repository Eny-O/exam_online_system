package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.entity.Question;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 题目管理服务接口
 */
public interface IQuestionManagerService extends IService<Question> {
    /**
     * 条件分页查询题目列表
     */
    PageResult<QuestionVO> conditionPageQuery(QuestionQueryDTO queryDTO);
}
