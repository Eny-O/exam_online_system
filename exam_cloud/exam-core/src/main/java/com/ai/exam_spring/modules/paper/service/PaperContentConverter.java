package com.ai.exam_spring.modules.paper.service;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.pojo.vo.StuQuestionCompleteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 试卷内容转换组件：把含正确答案与解析的试卷内容转换为学生视图
 */
@Component
@RequiredArgsConstructor
public class PaperContentConverter {

    /**
     * 将试卷内容转换为学生视图
     *
     * @param paperContentVO 试卷内容
     * @return 学生视图的试卷内容
     */
    public StuPaperVO toStudentPaper(PaperContentVO paperContentVO) {
        StuPaperVO stuPaperVO = new StuPaperVO();
        BeanUtil.copyProperties(paperContentVO, stuPaperVO);
        List<StuPaperQuestionVO> stuPaperQuestionVOS = paperContentVO.getPaperQuestionVOS().stream()
                .map(this::toStudentPaperQuestion)
                .toList();
        stuPaperVO.setStuPaperQuestionVOS(stuPaperQuestionVOS);
        return stuPaperVO;
    }

    /**
     * 将试卷题目转换为学生视图
     *
     * @param paperQuestionVO 试卷题目
     * @return 学生视图的试卷题目
     */
    private StuPaperQuestionVO toStudentPaperQuestion(PaperQuestionVO paperQuestionVO) {
        StuPaperQuestionVO stuPaperQuestionVO = new StuPaperQuestionVO();
        BeanUtil.copyProperties(paperQuestionVO, stuPaperQuestionVO);
        QuestionCompleteVO questionCompleteVO = paperQuestionVO.getQuestionCompleteVO();
        if (questionCompleteVO != null) {
            StuQuestionCompleteVO stuQuestionCompleteVO = new StuQuestionCompleteVO();
            stuQuestionCompleteVO.setId(questionCompleteVO.getId());
            stuQuestionCompleteVO.setContent(questionCompleteVO.getContent());
            stuQuestionCompleteVO.setType(questionCompleteVO.getType() == null
                    ? null : questionCompleteVO.getType().getCode());
            stuQuestionCompleteVO.setOptions(questionCompleteVO.getOptions());
            stuPaperQuestionVO.setStuQuestionCompleteVO(stuQuestionCompleteVO);
        }
        return stuPaperQuestionVO;
    }
}
