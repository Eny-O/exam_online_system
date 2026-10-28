package com.ai.exam_spring.modules.paper.mapper;

import com.ai.exam_spring.modules.paper.pojo.dos.PaperQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {
    void insertBatch(List<PaperQuestion> paperQuestions);
}
