package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.domain.dto.QuestionCompleteDTO;
import com.ai.exam_spring.domain.dto.QuestionOptionDTO;
import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.entity.Question;
import com.ai.exam_spring.domain.entity.QuestionOption;
import com.ai.exam_spring.domain.vo.QuestionOptionVO;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.ai.exam_spring.mapper.QuestionManagementMapper;
import com.ai.exam_spring.domain.vo.QuestionCompleteVO;
import com.ai.exam_spring.mapper.QuestionOptionMapper;
import com.ai.exam_spring.service.QuestionManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 题目管理服务实现
 */
@Service
public class QuestionManagementServiceImpl implements QuestionManagementService {

    @Autowired
    private QuestionManagementMapper questionManagementMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;


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
        Page<Question> questionPage = questionManagementMapper.selectPage(page, wrapper);
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

    /**
     * 添加完整题目 更新完整题目
     * 先添加题目,再添加选项,采用事务回滚保证数据一致性
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addQuestionComplete(QuestionCompleteDTO questionCompleteDTO) {
        Question question = new Question();
        BeanUtil.copyProperties(questionCompleteDTO, question);
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        question.setCreatorId(Integer.valueOf(currentUserId));
        if (question.getId() == null) {
            questionManagementMapper.insert(question);
        } else {
            questionManagementMapper.updateById(question);
            //TODO 当为简答或判断题时,options可能为空,导致原本的数据被删除了
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId,question.getId()));
        }
        List<QuestionOptionDTO> options = questionCompleteDTO.getOptions();
        if (options != null && !options.isEmpty()) {
            options.forEach(questionOptionDTO -> {
                QuestionOption questionOption = new QuestionOption();
                BeanUtil.copyProperties(questionOptionDTO, questionOption);
                questionOption.setQuestionId(question.getId());
                questionOptionMapper.insert(questionOption);
            });
        }
    }
    /**
     * 根据题目ID查询完整题目
     * 先查题,再查询选项,最后封装返回
     */
    @Override
    public QuestionCompleteVO getQuestionComplete(Integer id) {
        Question question = questionManagementMapper.selectById(id);
        List<QuestionOption> optionList = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
        QuestionCompleteVO questionCompleteVO = new QuestionCompleteVO();
        BeanUtil.copyProperties(question, questionCompleteVO);
        List<QuestionOptionVO> questionOptionVOS = optionList.stream().map(option -> {
            QuestionOptionVO questionOptionVO = new QuestionOptionVO();
            BeanUtil.copyProperties(option, questionOptionVO);
            return questionOptionVO;
        }).toList();
        questionCompleteVO.setOptions(questionOptionVOS);
        return questionCompleteVO;
    }

    /**
     * 批量删除完整题目
     * 先删除选项,再删除题目
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeQuestionCompleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        //先删除选项
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().in(QuestionOption::getQuestionId,ids));
        //后删除题目
        questionManagementMapper.deleteByIds(ids);
    }

}
