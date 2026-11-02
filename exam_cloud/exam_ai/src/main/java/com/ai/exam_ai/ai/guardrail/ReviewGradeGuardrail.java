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
 * 复核结果输出护栏：
 * - 总分不守恒、偏差标记含非法值等可确定性修正的问题，由代码核算后改写输出直接返回，不让模型重跑；
 * - 动作非法、要点缺失、与初评要点不一致等无法自动修正的问题，以 reprompt 方式让模型带纠正提示重新输出。
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
            review = objectMapper.readValue(GuardrailJsonSupport.extractJson(text), ReviewGradeResult.class);
        } catch (Exception e) {
            return reprompt("复核输出不是合法 JSON：" + e.getMessage(),
                    "请只输出约定的 JSON 结构，不要使用 markdown 代码块包裹，也不要输出任何额外说明文字。");
        }

        // 状态与裁决缺失无法自动修正，直接让模型重来
        List<String> problems = new ArrayList<>();
        String status = review.getStatus();
        if (!GradeStatus.GRADED.name().equals(status) && !GradeStatus.UNGRADABLE.name().equals(status)) {
            problems.add("status 非法：" + status);
        }
        if (GradeStatus.UNGRADABLE.name().equals(status)) {
            if (review.getFinalScore() != null) {
                problems.add("status=UNGRADABLE 时不应给出 finalScore");
            }
            return reprompt(String.join("；", problems), buildRepromptPrompt(problems, List.of()));
        }
        List<PointDecision> decisions = review.getDecisions();
        if (decisions == null || decisions.isEmpty()) {
            problems.add("status=GRADED 时 decisions 不能为空");
            return reprompt(String.join("；", problems), buildRepromptPrompt(problems, List.of()));
        }

        // 可确定性修正：总分不守恒、偏差标记含非法值
        List<String> fixed = new ArrayList<>();
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
                List<String> legalFlags = decision.getBiasFlags().stream()
                        .filter(ALLOWED_BIAS_FLAGS::contains).toList();
                if (legalFlags.size() != decision.getBiasFlags().size()) {
                    fixed.add("pointNo=" + decision.getPointNo() + " 的 biasFlags 已剔除非法值");
                    decision.setBiasFlags(legalFlags);
                }
            }
        }
        if (review.getFinalScore() == null) {
            fixed.add("finalScore 已按各要点得分之和自动补齐为 " + scoreSum);
            review.setFinalScore(scoreSum);
        } else if (Math.abs(scoreSum - review.getFinalScore()) > SCORE_TOLERANCE) {
            fixed.add("finalScore 由 " + review.getFinalScore() + " 修正为各要点得分之和 " + scoreSum);
            review.setFinalScore(scoreSum);
        }
        if (review.getBiasFlags() != null) {
            List<String> legalFlags = review.getBiasFlags().stream()
                    .filter(ALLOWED_BIAS_FLAGS::contains).toList();
            if (legalFlags.size() != review.getBiasFlags().size()) {
                fixed.add("biasFlags 已剔除非法值");
                review.setBiasFlags(legalFlags);
            }
        }

        // 与初评要点一一对应
        InitialGradeResult initial = initialGradeOf(request);
        if (initial != null && initial.getRubricPoints() != null) {
            Set<Integer> expectedPointNos = new LinkedHashSet<>();
            initial.getRubricPoints().forEach(point -> expectedPointNos.add(point.getPointNo()));
            if (!expectedPointNos.equals(actualPointNos)) {
                problems.add("decisions 的要点集合 " + actualPointNos + " 与初评要点集合 " + expectedPointNos
                        + " 不一致（不得新增或删除要点）");
            }
        }

        if (problems.isEmpty()) {
            if (!fixed.isEmpty()) {
                String corrected = GuardrailJsonSupport.toJson(objectMapper, review);
                if (corrected != null) {
                    log.info("[护栏] 复核结果已自动修正：{}", String.join("；", fixed));
                    return successWith(corrected);
                }
                problems.add("系统已按裁决明细自动核算分数，但改写结果序列化失败，请重新输出完整 JSON");
            } else {
                log.info("[护栏] 复核结果校验通过：status={} finalScore={} 裁决要点数={}",
                        review.getStatus(), review.getFinalScore(), decisions.size());
                return success();
            }
        }
        log.warn("[护栏] 复核结果未通过校验：{}", String.join("；", problems));
        return reprompt(String.join("；", problems), buildRepromptPrompt(problems, fixed));
    }

    /**
     * 从模板变量中取初评结果；取不到则跳过要点覆盖校验，不误判
     */
    private InitialGradeResult initialGradeOf(OutputGuardrailRequest request) {
        Map<String, Object> variables = request.requestParams().variables();
        Object value = variables == null ? null : variables.get("initialGrade");
        return value instanceof InitialGradeResult initial ? initial : null;
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
    private String buildRepromptPrompt(List<String> problems, List<String> fixed) {
        StringBuilder builder = new StringBuilder("本次复核结果未通过校验，请修正后重新输出完整 JSON：\n");
        for (String problem : problems) {
            builder.append("- ").append(problem).append('\n');
        }
        if (!fixed.isEmpty()) {
            builder.append("（系统已按裁决明细自动核算并修正：").append(String.join("；", fixed))
                    .append("，请保持一致）\n");
        }
        return builder.append("要求：decisions 必须与初评要点一一对应（不得新增或删除要点），")
                .append("action 只能取 KEEP/UP/DOWN，finalScore 必须等于各要点 finalScore 之和。")
                .toString();
    }
}
