package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.entity.Question;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IQuestionManagerService extends IService<Question> {
    PageResult<QuestionVO> conditionPageQuery(QuestionQueryDTO queryDTO);
}
