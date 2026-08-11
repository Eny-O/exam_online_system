package com.ai.exam_spring.service.exam.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperQuestionVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.pojo.vo.question.StuQuestionCompleteVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.StuExamService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StuExamServiceImpl implements StuExamService {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private IPaperManagementService paperManagementService;

    @Override
    public StuExamVO examInfoById(Integer id) {
        Exam exam = examManagementService.getById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        StuExamVO stuExamVO = new StuExamVO();
        BeanUtil.copyProperties(exam, stuExamVO);
        return stuExamVO;
    }

    /**
     * 根据id获取试卷信息
     * @param id
     * @return StuPaperVO
     */
    @Override
    public StuPaperVO getPaperById(Integer id) {
        PaperContentVO paperContentVO = paperManagementService.getPaperById(id);
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        StuPaperVO stuPaperVO = new StuPaperVO();
        BeanUtil.copyProperties(paperContentVO, stuPaperVO);
        List<StuPaperQuestionVO> stuPaperQuestionVOS = paperContentVO.getPaperQuestionVOS().stream().map(paperQuestionVO -> {
            StuPaperQuestionVO stuPaperQuestionVO = new StuPaperQuestionVO();
            BeanUtil.copyProperties(paperQuestionVO, stuPaperQuestionVO);
            StuQuestionCompleteVO stuQuestionCompleteVO = new StuQuestionCompleteVO();
            BeanUtil.copyProperties(paperQuestionVO.getQuestionCompleteVO(), stuQuestionCompleteVO);
            stuPaperQuestionVO.setStuQuestionCompleteVO(stuQuestionCompleteVO);
            return stuPaperQuestionVO;
        }).toList();
        stuPaperVO.setStuPaperQuestionVOS(stuPaperQuestionVOS);
        return stuPaperVO;
    }
}
