package com.ai.exam_ai.ai.guardrail;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.PointDecision;
import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import com.ai.exam_ai.pojo.enums.GradeStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 复核结果输出护栏：校验复核结果的自洽性与业务规则，
 * 不通过时以 reprompt 方式让模型按纠正提示重新输出，而不是直接判为不可批阅。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewGradeGuardrail implements OutputGuardrail {

    /** 允许的裁决动作 */
    private static final Set<String> ALLOWED_ACTIONS = Set.of("KEEP", "UP", "DOWN");

    /** 允许的偏差标记 */
    private static final Set<String> ALLOWED_BIAS_FLAGS = Set.of(
            "REFERENCE_WORDING_ANCHORING", "MISSING_RUBRIC_POINT", "DOUBLE_COUNTING", "VERBOSITY_BIAS",
            "FORMATTING_BIAS", "FLUENCY_BIAS", "LENIENCY_OR_STRICTNESS", "IRRELEVANT_OR_COPIED");

    /** 分数比较容差 */
    private static final double SCORE_TOLERANCE = 1e-6;

    private final ObjectMapper objectMapper;

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        String text = request.responseFromLLM().aiMessage().text();
        log.info("[护栏] 触发复核输出校验：输出长度={} 模板变量={}",
                text == null ? 0 : text.length(), templateVariableKeys(request));
        ReviewGradeResult review;
        try {
            review = objectMapper.readValue(extractJson(text), ReviewGradeResult.class);
        } catch (Exception e) {
            return reprompt("复核输出不是合法 JSON：" + e.getMessage(),
                    "请只输出约定的 JSON 结构，不要使用 markdown 代码块包裹，也不要输出任何额外说明文字。");
        }

        List<String> problems = collectProblems(review, request);
        if (problems.isEmpty()) {
            return success();
        }
        log.warn("[护栏] 复核结果未通过校验：{}", String.join("；", problems));
        return reprompt(String.join("；", problems), buildRepromptPrompt(problems));
    }

    /**
     * 逐条校验复核结果
     */
    private List<String> collectProblems(ReviewGradeResult review, OutputGuardrailRequest request) {
        List<String> problems = new ArrayList<>();
        String status = review.getStatus();
        if (!GradeStatus.GRADED.name().equals(status) && !GradeStatus.UNGRADABLE.name().equals(status)) {
            problems.add("status 非法：" + status);
        }
        if (GradeStatus.UNGRADABLE.name().equals(status) && review.getFinalScore() != null) {
            problems.add("status=UNGRADABLE 时不应给出 finalScore");
        }
        if (!GradeStatus.GRADED.name().equals(status)) {
            return problems;
        }

        List<PointDecision> decisions = review.getDecisions();
        if (decisions == null || decisions.isEmpty()) {
            problems.add("status=GRADED 时 decisions 不能为空");
            return problems;
        }

        double scoreSum = 0;
        Set<Integer> actualPointNos = new LinkedHashSet<>();
        for (PointDecision decision : decisions) {
            if (decision.getPointNo() == null) {
                problems.add("存在缺少 pointNo 的裁决");
            } else {
                actualPointNos.add(decision.getPointNo());
            }
            if (decision.getAction() == null || !ALLOWED_ACTIONS.contains(decision.getAction().toUpperCase())) {
                problems.add("action 非法：" + decision.getAction());
            }
            if (decision.getFinalScore() == null) {
                problems.add("pointNo=" + decision.getPointNo() + " 缺少 finalScore");
            } else {
                scoreSum += decision.getFinalScore();
            }
            if (decision.getBiasFlags() != null) {
                for (String flag : decision.getBiasFlags()) {
                    if (!ALLOWED_BIAS_FLAGS.contains(flag)) {
                        problems.add("biasFlags 含非法值：" + flag);
                    }
                }
            }
        }

        if (review.getFinalScore() == null) {
            problems.add("缺少 finalScore");
        } else if (Math.abs(scoreSum - review.getFinalScore()) > SCORE_TOLERANCE) {
            problems.add("finalScore=" + review.getFinalScore() + " 与各要点得分之和 " + scoreSum + " 不一致");
        }

        InitialGradeResult initial = initialGradeOf(request);
        if (initial != null && initial.getRubricPoints() != null) {
            Set<Integer> expectedPointNos = new LinkedHashSet<>();
            initial.getRubricPoints().forEach(point -> expectedPointNos.add(point.getPointNo()));
            if (!expectedPointNos.equals(actualPointNos)) {
                problems.add("decisions 的要点集合 " + actualPointNos
                        + " 与初评要点集合 " + expectedPointNos + " 不一致（不得新增或删除要点）");
            }
        }
        return problems;
    }

    /**
     * 从模板变量中取初评结果；取不到则跳过要点覆盖校验，不误判
     */
    private InitialGradeResult initialGradeOf(OutputGuardrailRequest request) {
        Map<String, Object> variables = request.requestParams().variables();
        Object value = variables == null ? null : variables.get("initialGrade");
        return value instanceof InitialGradeResult initial ? initial : null;
    }

    /**
     * 取模板变量名，用于确认初评结果是否随模板变量传入
     */
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
        StringBuilder builder = new StringBuilder("本次复核结果未通过校验，请修正后重新输出完整 JSON：\n");
        for (String problem : problems) {
            builder.append("- ").append(problem).append('\n');
        }
        return builder.append("要求：decisions 必须与初评要点一一对应（不得新增或删除要点），")
                .append("action 只能取 KEEP/UP/DOWN，finalScore 必须等于各要点 finalScore 之和。")
                .toString();
    }

    /**
     * 从模型输出中提取 JSON 片段：兼容 markdown 代码块围栏与前后说明文字
     */
    private String extractJson(String text) {
        return GuardrailJsonSupport.extractJson(text);
    }
}
