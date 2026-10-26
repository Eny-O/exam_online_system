package com.ai.exam_spring.modules.paper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.modules.paper.pojo.dto.BasePaperDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperQueryDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperQuestionDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperStatusUpdateDTO;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import com.ai.exam_spring.modules.paper.pojo.dos.Paper;
import com.ai.exam_spring.modules.paper.pojo.dos.PaperQuestion;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.paper.mapper.PaperManagementMapper;
import com.ai.exam_spring.modules.paper.mapper.PaperQuestionMapper;
import com.ai.exam_spring.modules.validation.impl.DeleteValidationServiceImpl;
import com.ai.exam_spring.modules.question.service.QuestionManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 试卷表 服务实现类
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
@Service
public class PaperManagementServiceImpl extends ServiceImpl<PaperManagementMapper, Paper> implements IPaperManagementService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionManagementService questionManagementService;

    @Autowired
    private DeleteValidationServiceImpl deleteValidationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PageResult<PaperVO> pagePaper(PaperQueryDTO paperQueryDTO) {
        Page<PaperVO> page = new Page<>(paperQueryDTO.getPageNum(), paperQueryDTO.getPageSize());
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .like(StringUtils.isNotBlank(paperQueryDTO.getName()), Paper::getName, paperQueryDTO.getName())
                .apply(paperQueryDTO.getPaperStatus() != null, "p.status = {0}", paperQueryDTO.getPaperStatus());
        Page<PaperVO> paperVOPage = baseMapper.pagePaper(page, wrapper);
        return PageResult.of(paperVOPage.getTotal(), paperVOPage.getRecords(), paperQueryDTO.getPageNum(), paperQueryDTO.getPageSize());
    }

    /**
     * 添加/更新试卷
     * 当前用户存在才做处理
     * 添加: 先添加试卷基本信息，再添加试卷题目信息到试卷题目关联表
     * 更新: 更新试卷的基本信息,先删除试卷题目关联表数据，再添加新的试卷题目关联表数据
     * @param paperUpdateDTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addPaper(BasePaperDTO paperUpdateDTO) {
        // 安全校验并获取当前用户的 id
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        Paper paper = new Paper();
        BeanUtil.copyProperties(paperUpdateDTO, paper);
        Integer paperId = paper.getId();
        System.out.println(paperId);
        // 没有id做新增逻辑,有id做更新逻辑
        if (paperId == null) {
            baseMapper.insert(paper);
        }else {
            Paper existPaper = baseMapper.selectById(paper.getId());
            if (existPaper ==  null) throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
            // 状态为已启用的试卷不能更新,确保原子性
            if (Objects.equals(existPaper.getStatus(), PaperStatus.ALREADY_ENABLE)) {
                throw new BusinessException(ErrorCodeEnum.PAPER_ALREADY_ENABLE_NOT_UPDATE);
            }
            paper.setVersion(existPaper.getVersion());
            // 只有当id和当前状态与数据库一致,才能更新(已配置基于版本号的乐观锁),确保原子性
            Wrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                    .eq(Paper::getId, paperId)
                    .eq(Paper::getStatus, existPaper.getStatus());
            int result  = baseMapper.update(paper, wrapper);
            if (result <= 0) throw new BusinessException(ErrorCodeEnum.PLEASE_FLUSH_AFTER_RETRY);
            paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
        }

        // 新增试卷题目关联表数据
        Integer BackPaperId = paper.getId();
        List<PaperQuestionDTO> paperQuestionDTOS = paperUpdateDTO.getQuestions();
        // 题目列表为空则不新增试卷题目关联表数据,并将状态设为草稿
        if (paperQuestionDTOS == null || paperQuestionDTOS.isEmpty()) {
            baseMapper.update(
                    new LambdaUpdateWrapper<Paper>()
                            .set(Paper::getStatus, PaperStatus.DRAFT)
                            .eq(Paper::getId, BackPaperId));
            return;
        }
        List<PaperQuestion> paperQuestions = paperQuestionDTOS.stream().map(paperQuestionDTO -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            BeanUtil.copyProperties(paperQuestionDTO, paperQuestion);
            paperQuestion.setPaperId(BackPaperId);
            return paperQuestion;
        }).toList();
        paperQuestionMapper.insertBatch(paperQuestions);
    }

    /**
     * 获取试卷内容
     * 先获取试卷基本信息,再通过试卷题目关联表获取完整的题目列表
     * @param id
     * @return PaperContentVO
     */
    @Override
    public PaperContentVO getPaperById(Integer id) {
        // 获取试卷基本信息
        Paper paper = baseMapper.selectById(id);
        if (paper == null) throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        Integer paperId = paper.getId();
        PaperContentVO paperContentVO = new PaperContentVO();
        BeanUtil.copyProperties(paper, paperContentVO);
        // 获取试卷题目列表
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
        if (paperQuestions == null || paperQuestions.isEmpty()) return paperContentVO;
        List<Integer> questionIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).toList();
        // 通过questionIds 获取所有题目
        List<QuestionCompleteVO> questionCompleteVOS = questionManagementService.getQuestionCompleteByIds(questionIds);
        if (questionCompleteVOS == null || questionCompleteVOS.isEmpty()) return paperContentVO;
        // 映射 questiongId -> QuestionCompleteVO
        Map<Integer, QuestionCompleteVO> completeVOMap = questionCompleteVOS.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        QuestionCompleteVO::getId,
                        Function.identity(),
                        (existing, replacement) -> replacement // 重复时保留新值（或抛异常）
                ));
        // 映射组装题目,题序,分数
        List<PaperQuestionVO> paperQuestionVOS = paperQuestions.stream().map(paperQuestion -> {
            PaperQuestionVO paperQuestionVO = new PaperQuestionVO();
            paperQuestionVO.setScore(paperQuestion.getScore());
            paperQuestionVO.setSortOrder(paperQuestion.getSortOrder());
            // 获取题目
            QuestionCompleteVO questionCompleteVO = completeVOMap.get(paperQuestion.getQuestionId());
            paperQuestionVO.setQuestionCompleteVO(questionCompleteVO);
            return paperQuestionVO;
        }).toList();

        paperContentVO.setPaperQuestionVOS(paperQuestionVOS);
        return paperContentVO;
    }

    /**
     * 批量删除试卷
     * 先删除试卷题目关联表数据,再删除试卷基本信息
     * //TODO 后续可做逻辑删除
     * @param ids
     */
    @Override
    public void removePaperByIds(List<Integer> ids) {
        if (ids == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        List<Paper> dbPapers = baseMapper.selectList(new LambdaQueryWrapper<Paper>().in(Paper::getId, ids));
        // 批量删除的试卷有不存在的,不能删除
        if (dbPapers.size() != ids.size()) throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        // 批量删除的试卷若有已启用的试卷,不能删除
        boolean hasEnablePaper = dbPapers.stream().anyMatch(paper -> paper.getStatus() == PaperStatus.ALREADY_ENABLE);
        if (hasEnablePaper) {
            throw new BusinessException(ErrorCodeEnum.PAPER_ALREADY_ENABLE_NOT_DELETE);
        }
        boolean isDelete = deleteValidationService.validatePaperReferenceAnyExam(ids);
        // 判断试卷是否被其他试卷引用
        if (!isDelete) throw new BusinessException(ErrorCodeEnum.PAPER_REFERENCE_ANY_EXAM);

        for (Integer paperId : ids) {
            transactionTemplate.execute(status -> {
                // 删除试卷题目关联表数据
                paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
                // 删除试卷基本信息
                int i = baseMapper.delete(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getId,paperId)
                        .ne(Paper::getStatus, PaperStatus.ALREADY_ENABLE)); // 状态为已启用的试卷不能删除,确保原子性
                if (i <= 0) throw new BusinessException(ErrorCodeEnum.PLEASE_FLUSH_AFTER_RETRY);
                return null;
            });
        }
    }

    /**
     * 修改试卷状态
     * @param paperStatusUpdateDTO
     * TODO 后续完成考试基本业务后,添加试卷是否被引用的判断
     */
    @Override
    public void updatePaperStatus(PaperStatusUpdateDTO paperStatusUpdateDTO) {
        if (paperStatusUpdateDTO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Paper paper = baseMapper.selectById(paperStatusUpdateDTO.getId());
        if (paper == null) throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        PaperStatus existStatus = paper.getStatus();
        PaperStatus targetStatus = paperStatusUpdateDTO.getStatus();
        if (!existStatus.canTransferTo(targetStatus)) throw new BusinessException(ErrorCodeEnum.PAPER_STATUS_TRANSFER_NOT_ALLOWED);
        int i = baseMapper.update(
                new LambdaUpdateWrapper<Paper>()
                        .set(Paper::getStatus, targetStatus)
                        .eq(Paper::getId, paperStatusUpdateDTO.getId())
                        .eq(Paper::getStatus, existStatus));
        if (i <= 0) throw new BusinessException(ErrorCodeEnum.PAPER_STATUS_CHANGE_FAIL);
    }

    /**
     * 根据试卷id获取试卷的状态
     * @param paperId
     * @return
     */
    @Override
    public PaperStatus getPaperStatusById(Integer paperId) {
        if (paperId == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        Paper paper = baseMapper.selectById(paperId);
        return paper.getStatus();
    }


}
