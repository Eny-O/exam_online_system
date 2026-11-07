package com.ai.exam_spring.modules.question.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BatchValidationException;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_spring.modules.question.pojo.dos.Question;
import com.ai.exam_spring.modules.question.pojo.dos.QuestionOption;
import com.ai.exam_spring.modules.question.pojo.dto.*;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionOptionVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionVO;
import com.ai.exam_spring.modules.question.mapper.QuestionManagementMapper;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.mapper.QuestionOptionMapper;
import com.ai.exam_spring.modules.validation.DeleteValidationService;
import com.ai.exam_spring.modules.question.service.QuestionManagementService;
import com.ai.exam_spring.modules.question.support.QuestionSupport;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 题目管理服务实现
 */
@Slf4j
@Service
public class QuestionManagementServiceImpl implements QuestionManagementService {

    @Autowired
    private QuestionManagementMapper questionManagementMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Autowired
    private DeleteValidationService deleteValidationService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    private QuestionSupport questionSupport;


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
     * 检查待添加题目是否与题库或本批次已有题目重复
     */
    @Override
    public List<String> findDuplicateMessages(List<QuestionAddDTO> questionAddDTOS) {
        if (questionAddDTOS == null || questionAddDTOS.isEmpty()) return List.of();
        Set<String> contents = questionAddDTOS.stream()
                .map(dto -> dto == null || dto.getContent() == null ? "" : dto.getContent().trim())
                .filter(content -> !content.isEmpty())
                .collect(Collectors.toSet());
        Set<String> existing = questionSupport.loadExistingFingerprints(contents);
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < questionAddDTOS.size(); i++) {
            QuestionAddDTO dto = questionAddDTOS.get(i);
            if (dto == null || StringUtils.isBlank(dto.getContent()) || dto.getType() == null) continue;
            String fingerprint = questionSupport.buildFingerprint(dto.getType(), dto.getContent(),
                    questionSupport.optionContents(dto.getOptions()));
            if (!existing.add(fingerprint)) {
                messages.add("第" + (i + 1) + "条：题目重复（题干与选项内容与题库或本批次已有题目一致）");
            }
        }
        return messages;
    }

    /**
     * 批量添加题目
     * 捕获题目校验的
     * @param questionAddDTOS
     */
    @Override
    public BatchInsertResult addQuestionComplete(List<QuestionAddDTO> questionAddDTOS){
        int totalCount = questionAddDTOS.size();
        int successCount = 0;
        List<String> failMessages = new ArrayList<>();

        List<List<QuestionAddDTO>> batchList = splitBySize(questionAddDTOS, 500);
        for (int batchIndex = 0; batchIndex < batchList.size(); batchIndex++) {
            List<QuestionAddDTO> batch = batchList.get(batchIndex);
            int batchStart = batchIndex * 500 + 1;
            int batchEnd = batchStart + batch.size() - 1;
            try {
                insertQuestionComplete(batch);
                successCount += batch.size();
            } catch (Exception e) {
                log.error("第{}批(第{}~{}条)插入失败", batchIndex + 1, batchStart, batchEnd, e);
                failMessages.add(String.format(
                        "第%d批(第%d~%d条)插入失败，原因：%s",
                        batchIndex + 1, batchStart, batchEnd, e.getMessage()
                ));
            }
        }
        return new BatchInsertResult(totalCount, successCount, totalCount - successCount, failMessages);
    }

    /**
     * 把合集拆分为批次
     * @param list
     * @param batchSize
     * @return
     * @param <T>
     */
    private  <T> List<List<T>> splitBySize(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, end));
        }
        return batches;
    }

    /**
     * 分批次插入
     * 先添加题目,再添加选项,采用事务回滚保证数据一致性
     */
    public void insertQuestionComplete(List<QuestionAddDTO> questionAddDTOS) {
        // 校验参数
        if (questionAddDTOS == null || questionAddDTOS.isEmpty()) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        List<String> errors = new ArrayList<>();
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < questionAddDTOS.size(); i++) {
            QuestionAddDTO questionAddDTO = questionAddDTOS.get(i);
            List<QuestionOptionAddDTO> options = questionAddDTO.getOptions();
            String type = questionAddDTO.getType();
            // 校验题目字段与选项
            try {
                validateQuestionFields(questionAddDTO, options);
                validateOptions(type, options);
            } catch (BusinessException e) {
                errors.add("第"+(i+1)+"条: "+e.getMessage());
                continue;
            }
            // 添加题目和选项
            Question question = new Question();
            BeanUtil.copyProperties(questionAddDTO, question);
            questions.add(question);
        }
        if (!errors.isEmpty()) {
            throw new BatchValidationException(errors);
        }
        // 声明事务,包裹插入SQL
        transactionTemplate.execute(status -> {
            // 插入题目
            questionManagementMapper.insert(questions);
            // 将选项和对应的题目组合为 questionOptions
            List<QuestionOption> questionOptions = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                List<QuestionOptionAddDTO> optionAddDTOS = questionAddDTOS.get(i).getOptions();
                if (optionAddDTOS != null && !optionAddDTOS.isEmpty()) {
                    Integer questionId = questions.get(i).getId();
                    optionAddDTOS.forEach(optionAddDTO -> {
                        QuestionOption option = new QuestionOption();
                        BeanUtil.copyProperties(optionAddDTO, option);
                        option.setQuestionId(questionId);
                        questionOptions.add(option);
                    });
                }
            }
            // 有选项择批量插入选项
            if (!questionOptions.isEmpty()) {
                addOptions(questionOptions);
            }
            return null;
        });
    }

    /**
     * 校验题干、题型、难度与答案
     */
    private void validateQuestionFields(BaseQuestionDTO dto, List<? extends BaseQuestionOptionDTO> options) {
        if (dto == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        if (StringUtils.isBlank(dto.getContent())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "题干不能为空");
        }
        String type = dto.getType();
        if (type == null || !QuestionTypeEnum.exists(type)) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_TYPE_ERROR);
        }
        String difficulty = dto.getDifficulty();
        if (!questionSupport.isDifficultyAllowed(difficulty)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "难度无效，只允许 easy/medium/hard");
        }
        String answer = dto.getAnswer();
        if (StringUtils.isBlank(answer)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "正确答案不能为空");
        }
        String answerError = questionSupport.validateAnswer(type, answer, options);
        if (answerError != null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, answerError);
        }
    }

    /**
     * 校验单选/多选/判断类型的题目要有选项,选项数量不能超过6个,判断只能为2个选项,不符合抛出异常
     *
     * @param type
     * @param options
     */
    private <T> void validateOptions(String type, List<T> options) {
        // 判断题型是否存在,不存在抛出异常
        if (!QuestionTypeEnum.exists(type)) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_TYPE_ERROR);
        }
        // 校验选项
        boolean hasOptions = options != null && !options.isEmpty();
        if (type.equals(QuestionTypeEnum.MULTIPLE.getCode())
                && !hasOptions) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_NOT_EMPTY);
        }
        if (type.equals(QuestionTypeEnum.SINGLE.getCode())
                && !hasOptions) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_NOT_EMPTY);
        }
        if (type.equals(QuestionTypeEnum.JUDGE.getCode())
                && !hasOptions) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_NOT_EMPTY);
        }
        if (hasOptions && options.size() > 6) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_EXCEED_SIX);
        }
        if (hasOptions && type.equals(QuestionTypeEnum.JUDGE.getCode()) && options.size() != 2) {
            throw new BusinessException(ErrorCodeEnum.JUDGE_MUST_HAVE_TWO_OPTIONS);
        }
        if (hasOptions && options.size() < 2) {
            throw new BusinessException(ErrorCodeEnum.QUESTION_OPTIONS_AT_LEAST_TWO);
        }
    }

    /**
     * 截断字符串，防止错误信息过长
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "未知";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    /**
     * 批量添加选项
     *
     * @param questionOptions
     */
    private void addOptions(List<QuestionOption> questionOptions) {
        // options 已被校验,不会发生NPE
        questionOptions.forEach(questionOption -> {
            // 判断选项标签是否正确
            if (!OptionLabelEnum.exists(questionOption.getLabel().getCode())) {
                throw new BusinessException(ErrorCodeEnum.QUESTION_OPTION_LABEL_ERROR);
            }
        });
        questionOptionMapper.insert(questionOptions);
    }

    /**
     * 更新完整题目
     * 先校验参数,选项标签和类型,使用增量更新,更新选项,最后更新题目
     * // TODO: 这里可以做先删除后插入的更新操作的,现在使用的增量更新反而弄复杂了
     */
    @Override
    public void updateQuestionComplete(QuestionUpdateDTO questionUpdateDTO) {
        // 校验参数
        if (questionUpdateDTO == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        String type = questionUpdateDTO.getType();
        List<QuestionOptionUpdateDTO> optionDTOS = questionUpdateDTO.getOptions();
        validateQuestionFields(questionUpdateDTO, optionDTOS);
        // 校验选项
        validateOptions(type, optionDTOS);
        // 查询题目,获取版本号并设置版本号,准备好要插入的题目
        Integer questionId = questionUpdateDTO.getId();
        Question dbQuestion = questionManagementMapper.selectById(questionId);
        if (dbQuestion == null) throw new BusinessException(ErrorCodeEnum.QUESTION_NOT_EXIST);
        // 校验编辑后的题目是否与题库其他题目重复
        String updateContent = questionUpdateDTO.getContent() == null ? "" : questionUpdateDTO.getContent().trim();
        if (!updateContent.isEmpty()) {
            List<String> optionContents = QuestionTypeEnum.ESSAY.getCode().equals(type)
                    ? List.of()
                    : questionSupport.optionContents(questionUpdateDTO.getOptions());
            String fingerprint = questionSupport.buildFingerprint(type, updateContent, optionContents);
            Set<String> existing = questionSupport.loadExistingFingerprints(Set.of(updateContent), questionId);
            if (existing.contains(fingerprint)) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,
                        "题目重复（题干与选项内容与题库已有题目一致）");
            }
        }
        Question newQuestion = new Question();
        BeanUtil.copyProperties(questionUpdateDTO, newQuestion);
        newQuestion.setVersion(dbQuestion.getVersion());
        transactionTemplate.execute(status -> {
            // 简答题删除所有选项
            if (type.equals(QuestionTypeEnum.ESSAY.getCode())) {
                questionOptionMapper.delete(
                        new LambdaQueryWrapper<QuestionOption>()
                                .eq(QuestionOption::getQuestionId, questionId));
            } else {
                // 选择题型,增量更新选项
                // 获取数据库关联的选项集
                List<QuestionOption> dbOptions = questionOptionMapper.selectList(
                        new LambdaQueryWrapper<QuestionOption>()
                                .eq(QuestionOption::getQuestionId, questionId));
                // 没有选项,是原简答题变为选择题型,执行批量插入选项
                if (dbOptions.isEmpty()) {
                    List<QuestionOption> questionOptions = questionUpdateDTO.getOptions().stream().map(optionUpdateDTO -> {
                        QuestionOption questionOption = new QuestionOption();
                        BeanUtil.copyProperties(optionUpdateDTO, questionOption);
                        questionOption.setQuestionId(questionId);
                        return questionOption;
                    }).toList();

                    addOptions(questionOptions);
                } else {
                    // 有选项,选择题型之间的更新
                    Map<Integer, QuestionOption> dbOptionMap = dbOptions.stream()
                            .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));
                    Set<Integer> ids = optionDTOS.stream().map(QuestionOptionUpdateDTO::getId).collect(Collectors.toSet());
                    // 数据库有的选项,但参数里没有的选项,加入删除集合
                    List<Integer> deleteIds = dbOptions.stream().map(QuestionOption::getId).filter(id -> !ids.contains(id)).toList();
                    // 参数里有的,数据库里也有的,则加入更新集合
                    List<QuestionOption> updateOptions = new ArrayList<>();
                    // 参数里有的,但数据库没有的,则加入插入集合
                    List<QuestionOption> insertOptions = new ArrayList<>();
                    optionDTOS.forEach(optionDTO -> {
                        QuestionOption option = new QuestionOption();
                        BeanUtil.copyProperties(optionDTO, option);
                        option.setQuestionId(questionId);
                        if (optionDTO.getId() != null && dbOptionMap.containsKey(optionDTO.getId())) {
                            updateOptions.add(option);
                        } else {
                            // 为了调用能检验选项标签是否有错误的 addOptions方法,这里做了类型转换的封装
                            insertOptions.add(option);
                        }
                    });
                    // 增量更新上述集合
                    if (!updateOptions.isEmpty()) {
                        questionOptionMapper.updateById(updateOptions);
                    }
                    if (!insertOptions.isEmpty()) {
                        addOptions(insertOptions);
                    }
                    if (!deleteIds.isEmpty()) {
                        questionOptionMapper.deleteByIds(deleteIds);
                    }
                }
            }
            // 更新题目(已配置mp的基于version的乐观锁插件)
            int i = questionManagementMapper.update(newQuestion,
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getId, questionId));
            if (i <= 0) throw new BusinessException(ErrorCodeEnum.DATA_ALREADY_MODIFIED_PLEASE_FLUSH_AFTER_RETRY);
            return null;
        });
    }

    /**
     * 根据题目ID查询完整题目
     * 先查题,再查询选项,最后封装返回
     */
    @Override
    public QuestionCompleteVO getQuestionComplete(Integer id) {
        Question question = questionManagementMapper.selectById(id);
        List<QuestionOption> optionList = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
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
        boolean isDelete = deleteValidationService.validateQuestionsReferenceAnyPaper(ids);
        if (!isDelete) throw new BusinessException(ErrorCodeEnum.QUESTION_REFERENCE_PAPER);
        //先删除选项
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().in(QuestionOption::getQuestionId, ids));
        //后删除题目
        questionManagementMapper.deleteByIds(ids);
    }

    /**
     * 批量查询完整题目
     * @param ids
     * @return List<QuestionCompleteVO>
     */
    @Override
    public List<QuestionCompleteVO> getQuestionCompleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Question> questions = questionManagementMapper.selectByIds(ids);
        if (questions == null || questions.isEmpty()) return Collections.emptyList();
        List<Integer> idList = questions.stream().map(Question::getId).toList();
        // 获取所有选项
        List<QuestionOption> allOptions = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>().in(QuestionOption::getQuestionId, idList));
        Map<Integer, List<QuestionOption>> map = Collections.emptyMap();
        if (allOptions != null && !allOptions.isEmpty()) {
            // 映射题目与其对应的选项
            map = allOptions.stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        }
        Map<Integer, List<QuestionOption>> questionOptiionsMap = map;

        // 将questions转换为QuestionCompleteVOS,并将选项转换为QuestionOptionVOS,并返回完整的题目列表
        return questions.stream().map(question -> {
            QuestionCompleteVO questionCompleteVO = new QuestionCompleteVO();
            BeanUtil.copyProperties(question, questionCompleteVO);
            // 拿到选项
            List<QuestionOption> questionOptions = questionOptiionsMap.getOrDefault(question.getId(), Collections.emptyList());
            if (questionOptions.isEmpty()) return questionCompleteVO;
            // 将questionOptions转换为QuestionOptionVOS
            List<QuestionOptionVO> questionOptionVOS = questionOptions.stream().map(questionOption -> {
                QuestionOptionVO questionOptionVO = new QuestionOptionVO();
                BeanUtil.copyProperties(questionOption, questionOptionVO);
                return questionOptionVO;
            }).toList();
            questionCompleteVO.setOptions(questionOptionVOS);
            return questionCompleteVO;
        }).toList();
    }

}
