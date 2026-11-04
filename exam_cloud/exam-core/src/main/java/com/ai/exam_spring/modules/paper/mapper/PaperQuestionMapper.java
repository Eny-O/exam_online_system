package com.ai.exam_spring.modules.paper.mapper;

import com.ai.exam_spring.modules.paper.pojo.dos.PaperQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {
    void insertBatch(List<PaperQuestion> paperQuestions);

    /**
     * 查询某张试卷下各题目的ID与分值
     *
     * @param paperId 试卷ID
     * @return 题目ID与分值列表
     */
    List<PaperQuestion> selectByPaperId(Integer paperId);
}
