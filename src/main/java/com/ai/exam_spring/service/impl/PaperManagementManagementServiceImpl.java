package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.dto.PaperContentDTO;
import com.ai.exam_spring.domain.dto.PaperQueryDTO;
import com.ai.exam_spring.domain.dto.PaperQuestionDTO;
import com.ai.exam_spring.domain.entity.Paper;
import com.ai.exam_spring.domain.entity.PaperQuestion;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.ai.exam_spring.mapper.PaperManagementMapper;
import com.ai.exam_spring.mapper.PaperQuestionMapper;
import com.ai.exam_spring.service.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 试卷表 服务实现类
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
@Service
public class PaperManagementManagementServiceImpl extends ServiceImpl<PaperManagementMapper, Paper> implements IPaperManagementService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Override
    public PageResult<PaperVO> pagePaper(PaperQueryDTO paperQueryDTO) {
        Page<PaperVO> page = new Page<>(paperQueryDTO.getPageNum(), paperQueryDTO.getPageSize());
        QueryWrapper<Paper> wrapper = new QueryWrapper<Paper>().like("name", paperQueryDTO.getName());
        Page<PaperVO> paperVOPage = baseMapper.pagePaper(page, wrapper);
        return PageResult.of(paperVOPage.getTotal(), paperVOPage.getRecords(), paperQueryDTO.getPageNum(), paperQueryDTO.getPageSize());
    }

    /**
     * 添加试卷
     * 先添加试卷基本信息，再添加试卷题目信息到试卷题目关联表
     * @param paperContentDTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addPaper(PaperContentDTO paperContentDTO) {
        Paper paper = new Paper();
        BeanUtil.copyProperties(paperContentDTO, paper);
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        paper.setCreatorId(Integer.parseInt(currentUserId));
        baseMapper.insert(paper);
        Integer paperId = paper.getId();
        List<PaperQuestionDTO> questions = paperContentDTO.getQuestions();
        List<PaperQuestion> paperQuestions = questions.stream().map(question -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            BeanUtil.copyProperties(question, paperQuestion);
            paperQuestion.setPaperId(paperId);
            return paperQuestion;
        }).toList();
        paperQuestionMapper.insertBatch(paperQuestions);
    }
}
