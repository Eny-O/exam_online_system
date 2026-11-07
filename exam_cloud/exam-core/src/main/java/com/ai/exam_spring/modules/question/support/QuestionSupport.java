package com.ai.exam_spring.modules.question.support;

import com.ai.exam_spring.modules.question.mapper.QuestionManagementMapper;
import com.ai.exam_spring.modules.question.mapper.QuestionOptionMapper;
import com.ai.exam_spring.modules.question.pojo.dos.Question;
import com.ai.exam_spring.modules.question.pojo.dos.QuestionOption;
import com.ai.exam_spring.modules.question.pojo.dto.BaseQuestionOptionDTO;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目校验与查重公共组件
 */
@Component
public class QuestionSupport {

    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("easy", "medium", "hard");

    private static final String SEPARATOR = "\u0001";

    @Autowired
    private QuestionManagementMapper questionManagementMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    /**
     * 判断难度取值是否合法
     */
    public boolean isDifficultyAllowed(String difficulty) {
        return difficulty != null && ALLOWED_DIFFICULTIES.contains(difficulty);
    }

    /**
     * 校验答案格式与选项标签，合法返回 null
     */
    public String validateAnswer(String type, String answer, List<? extends BaseQuestionOptionDTO> options) {
        if (answer == null || answer.trim().isEmpty()) return "正确答案不能为空";
        String value = answer.trim();
        if (QuestionTypeEnum.ESSAY.getCode().equals(type)) return null;
        if (!value.matches("[A-F](,[A-F])*")) {
            return "答案[" + value + "]格式错误，应为大写选项标签（判断题填 A 或 B），多选用半角逗号分隔且升序（如 A,C,D）";
        }
        String[] parts = value.split(",");
        Set<String> labels = options == null ? Set.of() : options.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getLabel() != null)
                .map(option -> option.getLabel().getCode())
                .collect(Collectors.toSet());
        if (QuestionTypeEnum.SINGLE.getCode().equals(type)
                || QuestionTypeEnum.JUDGE.getCode().equals(type)) {
            if (parts.length != 1) return "单选题/判断题答案只能是一个选项标签，实际为[" + value + "]";
            if (!labels.contains(parts[0])) return "答案标签[" + parts[0] + "]在选项中不存在";
            return null;
        }
        String normalized = Arrays.stream(parts).distinct().sorted().collect(Collectors.joining(","));
        if (!normalized.equals(value)) {
            return "多选题答案需为升序且不重复的标签（如 A,C,D），实际为[" + value + "]";
        }
        for (String part : parts) {
            if (!labels.contains(part)) return "答案标签[" + part + "]在选项中不存在";
        }
        return null;
    }

    /**
     * 提取选项内容
     */
    public List<String> optionContents(List<? extends BaseQuestionOptionDTO> options) {
        if (options == null) return List.of();
        return options.stream()
                .filter(Objects::nonNull)
                .map(BaseQuestionOptionDTO::getContent)
                .toList();
    }

    /**
     * 生成题目指纹：题型 + 题干 + 选项内容字典序拼接
     */
    public String buildFingerprint(String type, String content, List<String> optionContents) {
        String sortedOptions = (optionContents == null ? List.<String>of() : optionContents).stream()
                .map(value -> value == null ? "" : value.trim())
                .sorted()
                .collect(Collectors.joining(SEPARATOR));
        String safeType = type == null ? "" : type;
        String safeContent = content == null ? "" : content.trim();
        return safeType + SEPARATOR + safeContent + SEPARATOR + sortedOptions;
    }

    /**
     * 查询题库中已有题目的指纹
     */
    public Set<String> loadExistingFingerprints(Set<String> contents) {
        return loadExistingFingerprints(contents, null);
    }

    /**
     * 查询题库中已有题目的指纹，可排除指定题目
     */
    public Set<String> loadExistingFingerprints(Set<String> contents, Integer excludeId) {
        Set<String> fingerprints = new HashSet<>();
        if (contents == null || contents.isEmpty()) return fingerprints;
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .select(Question::getId, Question::getContent, Question::getType)
                .in(Question::getContent, contents);
        if (excludeId != null) {
            wrapper.ne(Question::getId, excludeId);
        }
        List<Question> candidates = questionManagementMapper.selectList(wrapper);
        if (candidates.isEmpty()) return fingerprints;
        List<Integer> ids = candidates.stream().map(Question::getId).toList();
        List<QuestionOption> dbOptions = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .select(QuestionOption::getQuestionId, QuestionOption::getContent)
                        .in(QuestionOption::getQuestionId, ids));
        Map<Integer, List<String>> optionMap = dbOptions.stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionId,
                        Collectors.mapping(QuestionOption::getContent, Collectors.toList())));
        for (Question question : candidates) {
            String type = question.getType() == null ? "" : question.getType().getCode();
            fingerprints.add(buildFingerprint(type, question.getContent(),
                    optionMap.getOrDefault(question.getId(), List.of())));
        }
        return fingerprints;
    }
}
