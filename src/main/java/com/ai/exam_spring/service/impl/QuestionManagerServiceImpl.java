package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.entity.Question;
import com.ai.exam_spring.mapper.QuestionManagerMapper;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.ai.exam_spring.service.IQuestionManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 题目管理服务实现
 */
@Service
public class QuestionManagerServiceImpl extends ServiceImpl<QuestionManagerMapper, Question> implements IQuestionManagerService {

    @Autowired
    private QuestionManagerMapper questionManagerMapper;

    /**
     * 根据题目内容、题型、难度条件分页查询
     */
    @Override
    public PageResult<QuestionVO> conditionPageQuery(QuestionQueryDTO queryDTO) {
        Page<Question> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .like(StringUtils.isNotBlank(queryDTO.getContent()), Question::getContent, queryDTO.getContent())
                .eq(StringUtils.isNotBlank(queryDTO.getType()), Question::getType, queryDTO.getType())
                .eq(StringUtils.isNotBlank(queryDTO.getDifficulty()), Question::getDifficulty, queryDTO.getDifficulty())
                .orderByDesc(Question::getCreateTime);
        Page<Question> questionPage = questionManagerMapper.selectPage(page, wrapper);
        List<QuestionVO> questionVOList = questionPage.getRecords().stream().map(question -> {
            QuestionVO questionVO = new QuestionVO();
            BeanUtil.copyProperties(question, questionVO);
            return questionVO;
        }).toList();

        return PageResult.of(
                questionPage.getTotal(),
                questionVOList,
                queryDTO.getPageNum(),
                queryDTO.getPageSize());
    }
}
