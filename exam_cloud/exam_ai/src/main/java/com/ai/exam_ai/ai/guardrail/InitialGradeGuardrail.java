package com.ai.exam_ai.ai.guardrail;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.RubricPoint;
import com.ai.exam_ai.pojo.enums.GradeStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 初评结果输出护栏：校验初评结果的自洽性与分数守恒，
 * 不通过时以 reprompt 方式让模型按纠正提示重新输出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialGradeGuardrail implements OutputGuardrail {

    /** 分数比较容差 */
    private static final double SCORE_TOLERANCE = 1e-6;

    private final ObjectMapper objectMapper;

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        String text = request.responseFromLLM().aiMessage().text();
        log.info("[护栏] 触发初评输出校验：输出长度={} 模板变量={}",
                text == null ? 0 : text.length(), templateVariableKeys(request));
        InitialGradeResult initial;
        try {
            initial = objectMapper.readValue(GuardrailJsonSupport.extractJson(text), InitialGradeResult.class);
        } catch (Exception e) {
            return reprompt("初评输出不是合法 JSON：" + e.getMessage(),
                    "请只输出约定的 JSON 结构，不要使用 markdown 代码块包裹，也不要输出任何额外说明文字。");
        }

        List<String> problems = collectProblems(initial, request);
        if (problems.isEmpty()) {
            log.info("[护栏] 初评结果校验通过：status={} preliminaryScore={} 要点数={}",
                    initial.getStatus(), initial.getPreliminaryScore(),
                    initial.getRubricPoints() == null ? 0 : initial.getRubricPoints().size());
            return success();
        }
        log.warn("[护栏] 初评结果未通过校验：{}", String.join("；", problems));
        return reprompt(String.join("；", problems), buildRepromptPrompt(problems));
    }

    /**
     * 逐条校验初评结果
     */
    private List<String> collectProblems(InitialGradeResult initial, OutputGuardrailRequest request) {
        List<String> problems = new ArrayList<>();
        String status = initial.getStatus();
        if (!GradeStatus.GRADED.name().equals(status) && !GradeStatus.UNGRADABLE.name().equals(status)) {
            problems.add("status 非法：" + status);
        }
        if (GradeStatus.UNGRADABLE.name().equals(status)) {
            if (initial.getPreliminaryScore() != null) {
                problems.add("status=UNGRADABLE 时不应给出 preliminaryScore");
            }
            return problems;
        }

        List<RubricPoint> points = initial.getRubricPoints();
        if (points == null || points.isEmpty()) {
            problems.add("status=GRADED 时 rubricPoints 不能为空");
            return problems;
        }

        double maxScoreSum = 0;
        double awardedScoreSum = 0;
        Set<Integer> pointNos = new HashSet<>();
        int expectedPointNo = 1;
        for (RubricPoint point : points) {
            if (point.getPointNo() == null) {
                problems.add("存在缺少 pointNo 的要点");
            } else {
                if (!pointNos.add(point.getPointNo())) {
                    problems.add("pointNo 重复：" + point.getPointNo());
                }
                if (!point.getPointNo().equals(expectedPointNo)) {
                    problems.add("pointNo 应从 1 开始连续，当前为 " + point.getPointNo() + "，期望 " + expectedPointNo);
                }
                expectedPointNo++;
            }
            if (point.getMaxScore() == null || point.getMaxScore() <= 0) {
                problems.add("pointNo=" + point.getPointNo() + " 的 maxScore 必须大于 0");
            } else {
                maxScoreSum += point.getMaxScore();
            }
            if (point.getAwardedScore() == null) {
                problems.add("pointNo=" + point.getPointNo() + " 缺少 awardedScore");
            } else {
                if (point.getMaxScore() != null
                        && point.getAwardedScore() > point.getMaxScore() + SCORE_TOLERANCE) {
                    problems.add("pointNo=" + point.getPointNo() + " 的 awardedScore=" + point.getAwardedScore()
                            + " 超过 maxScore=" + point.getMaxScore());
                }
                awardedScoreSum += point.getAwardedScore();
            }
        }

        if (initial.getPreliminaryScore() == null) {
            problems.add("缺少 preliminaryScore");
        } else if (Math.abs(awardedScoreSum - initial.getPreliminaryScore()) > SCORE_TOLERANCE) {
            problems.add("preliminaryScore=" + initial.getPreliminaryScore()
                    + " 与各要点得分之和 " + awardedScoreSum + " 不一致");
        }

        Double fullScore = fullScoreOf(request);
        if (fullScore != null && Math.abs(maxScoreSum - fullScore) > SCORE_TOLERANCE) {
            problems.add("各要点满分之和 " + maxScoreSum + " 与题目满分 " + fullScore + " 不一致");
        }
        return problems;
    }

    /**
     * 从模板变量中取题目满分；取不到则跳过满分一致性校验
     */
    private Double fullScoreOf(OutputGuardrailRequest request) {
        Map<String, Object> variables = request.requestParams().variables();
        Object value = variables == null ? null : variables.get("fullScore");
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Set<String> templateVariableKeys(OutputGuardrailRequest request) {
        if (request.requestParams() == null || request.requestParams().variables() == null) {
            return Set.of();
        }
        return request.requestParams().variables().keySet();
    }

    /**
     * 构造纠正提示，要求模型按规则重新输出
     */
    private String buildRepromptPrompt(List<String> problems) {
        StringBuilder builder = new StringBuilder("本次初评结果未通过校验，请修正后重新输出完整 JSON：\n");
        for (String problem : problems) {
            builder.append("- ").append(problem).append('\n');
        }
        return builder.append("要求：各要点满分之和必须等于题目满分，preliminaryScore 必须等于各要点得分之和，")
                .append("awardedScore 必须在 0 到 maxScore 之间，pointNo 从 1 开始连续。")
                .toString();
    }
}
