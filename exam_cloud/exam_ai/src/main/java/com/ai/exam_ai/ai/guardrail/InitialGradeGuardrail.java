package com.ai.exam_ai.ai.guardrail;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.ReferencePointCoverage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 初评结果输出护栏：
 * - 分数守恒、分数越界等可确定性修正的问题，由代码核算后改写输出直接返回，不让模型重跑；
 * - 状态非法、要点缺失、满分不一致等无法自动修正的问题，以 reprompt 方式让模型带纠正提示重新输出；
 * - 参考答案采分点覆盖情况（漏拆某个要求部分、凭空新增要点、可选项被当成必答项扣分）在此自动校验。
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

        // 状态与要点缺失无法自动修正，直接让模型重来
        List<String> problems = new ArrayList<>();
        String status = initial.getStatus();
        if (!GradeStatus.GRADED.name().equals(status) && !GradeStatus.UNGRADABLE.name().equals(status)) {
            problems.add("status 非法：" + status);
        }
        if (GradeStatus.UNGRADABLE.name().equals(status)) {
            if (initial.getPreliminaryScore() != null) {
                problems.add("status=UNGRADABLE 时不应给出 preliminaryScore");
            }
            return reprompt(String.join("；", problems), buildRepromptPrompt(problems, List.of()));
        }
        List<RubricPoint> points = initial.getRubricPoints();
        if (points == null || points.isEmpty()) {
            problems.add("status=GRADED 时 rubricPoints 不能为空");
            return reprompt(String.join("；", problems), buildRepromptPrompt(problems, List.of()));
        }

        // 可确定性修正：分数越界与总分不守恒
        List<String> fixed = new ArrayList<>();
        double awardedSum = 0;
        double maxScoreSum = 0;
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
                if (point.getMaxScore() != null && point.getAwardedScore() > point.getMaxScore() + SCORE_TOLERANCE) {
                    fixed.add("pointNo=" + point.getPointNo() + " 的 awardedScore 由 " + point.getAwardedScore()
                            + " 截断到 maxScore=" + point.getMaxScore());
                    point.setAwardedScore(point.getMaxScore());
                }
                awardedSum += point.getAwardedScore();
            }
        }
        if (initial.getPreliminaryScore() == null) {
            fixed.add("preliminaryScore 已按各要点得分之和自动补齐为 " + awardedSum);
            initial.setPreliminaryScore(awardedSum);
        } else if (Math.abs(awardedSum - initial.getPreliminaryScore()) > SCORE_TOLERANCE) {
            fixed.add("preliminaryScore 由 " + initial.getPreliminaryScore() + " 修正为各要点得分之和 " + awardedSum);
            initial.setPreliminaryScore(awardedSum);
        }

        Double fullScore = fullScoreOf(request);
        if (fullScore != null && Math.abs(maxScoreSum - fullScore) > SCORE_TOLERANCE) {
            problems.add("各要点满分之和 " + maxScoreSum + " 与题目满分 " + fullScore + " 不一致");
        }

        // 参考答案采分点覆盖校验：检测漏拆要求部分、凭空新增要点、可选项被当成必答项扣分
        problems.addAll(checkReferencePointCoverage(initial, points));

        if (problems.isEmpty()) {
            if (!fixed.isEmpty()) {
                String corrected = GuardrailJsonSupport.toJson(objectMapper, initial);
                if (corrected != null) {
                    log.info("[护栏] 初评结果已自动修正：{}", String.join("；", fixed));
                    return successWith(corrected);
                }
                problems.add("系统已按要点明细自动核算分数，但改写结果序列化失败，请重新输出完整 JSON");
            } else {
                log.info("[护栏] 初评结果校验通过：status={} preliminaryScore={} 要点数={} 参考采分点数={}",
                        initial.getStatus(), initial.getPreliminaryScore(), points.size(),
                        initial.getReferencePointCoverage() == null ? 0 : initial.getReferencePointCoverage().size());
                return success();
            }
        }
        log.warn("[护栏] 初评结果未通过校验：{}", String.join("；", problems));
        return reprompt(String.join("；", problems), buildRepromptPrompt(problems, fixed));
    }

    /**
     * 校验参考答案采分点覆盖情况：
     * - 采分点清单必须存在，且每条必答采分点都指明了覆盖它的评分要点（漏拆某个要求部分会在这里暴露）；
     * - 每个评分要点都必须有对应的参考答案采分点，不允许凭空新增要点；
     * - 只覆盖了参考答案中（可选）条目的要点，不得因学生未作答而扣分。
     */
    private List<String> checkReferencePointCoverage(InitialGradeResult initial, List<RubricPoint> points) {
        List<String> problems = new ArrayList<>();
        List<ReferencePointCoverage> coverages = initial.getReferencePointCoverage();
        if (coverages == null || coverages.isEmpty()) {
            problems.add("缺少 referencePointCoverage：必须先逐条列出参考答案中的每一条采分点，"
                    + "并标明它由哪些评分要点覆盖（coveredByPointNos）");
            return problems;
        }
        Set<Integer> rubricPointNos = new HashSet<>();
        for (RubricPoint point : points) {
            if (point.getPointNo() != null) {
                rubricPointNos.add(point.getPointNo());
            }
        }
        Set<Integer> coveredPointNos = new HashSet<>();
        Map<String, List<ReferencePointCoverage>> coveragesByAspect = new LinkedHashMap<>();
        for (ReferencePointCoverage coverage : coverages) {
            String label = coverage.getPointDesc() == null ? "(未描述)" : coverage.getPointDesc();
            boolean optional = Boolean.TRUE.equals(coverage.getOptional());
            if (coverage.getAspect() == null || coverage.getAspect().isBlank()) {
                problems.add("采分点『" + label + "』缺少所属要求部分（aspect）");
            } else {
                coveragesByAspect.computeIfAbsent(coverage.getAspect(), key -> new ArrayList<>()).add(coverage);
            }
            if (coverage.getPointDesc() == null || coverage.getPointDesc().isBlank()) {
                problems.add("存在没有内容（pointDesc）的参考答案采分点");
            }
            if (coverage.getCoveredByPointNos() == null || coverage.getCoveredByPointNos().isEmpty()) {
                // 可选项不要求单独设点，允许不指向任何评分要点；必答采分点必须有要点覆盖
                if (!optional) {
                    problems.add("参考答案采分点『" + label + "』没有任何评分要点覆盖，该采分点所在的要求部分被漏掉了");
                }
            } else {
                for (Integer pointNo : coverage.getCoveredByPointNos()) {
                    if (pointNo == null) {
                        problems.add("采分点『" + label + "』引用了空的评分要点序号");
                    } else if (!rubricPointNos.contains(pointNo)) {
                        problems.add("采分点『" + label + "』引用了不存在的评分要点序号 " + pointNo);
                    } else {
                        coveredPointNos.add(pointNo);
                    }
                }
            }
        }
        // 某个要求部分的必答采分点全部没被覆盖 = 该部分整体漏拆
        for (Map.Entry<String, List<ReferencePointCoverage>> entry : coveragesByAspect.entrySet()) {
            boolean anyRequiredCovered = entry.getValue().stream()
                    .filter(coverage -> !Boolean.TRUE.equals(coverage.getOptional()))
                    .anyMatch(coverage -> coverage.getCoveredByPointNos() != null
                            && !coverage.getCoveredByPointNos().isEmpty());
            boolean hasRequired = entry.getValue().stream()
                    .anyMatch(coverage -> !Boolean.TRUE.equals(coverage.getOptional()));
            if (hasRequired && !anyRequiredCovered) {
                problems.add("要求部分『" + entry.getKey() + "』的所有必答采分点都没有评分要点覆盖，该部分被整体漏拆");
            }
        }
        // 评分要点必须有参考答案采分点作为依据
        for (RubricPoint point : points) {
            if (point.getPointNo() != null && !coveredPointNos.contains(point.getPointNo())) {
                problems.add("评分要点 pointNo=" + point.getPointNo() + " 没有对应的参考答案采分点，不允许凭空新增要点");
            }
        }
        // 只覆盖（可选）条目的要点不应扣分
        for (RubricPoint point : points) {
            if (point.getPointNo() == null) {
                continue;
            }
            List<ReferencePointCoverage> related = coverages.stream()
                    .filter(coverage -> coverage.getCoveredByPointNos() != null
                            && coverage.getCoveredByPointNos().contains(point.getPointNo()))
                    .toList();
            if (related.isEmpty()) {
                continue;
            }
            boolean allOptional = related.stream().allMatch(coverage -> Boolean.TRUE.equals(coverage.getOptional()));
            if (allOptional && point.getAwardedScore() != null && point.getMaxScore() != null
                    && point.getAwardedScore() < point.getMaxScore() - SCORE_TOLERANCE) {
                problems.add("评分要点 pointNo=" + point.getPointNo()
                        + " 只覆盖了参考答案中标注为（可选）的条目，不得因学生未作答而扣分");
            }
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
    private String buildRepromptPrompt(List<String> problems, List<String> fixed) {
        StringBuilder builder = new StringBuilder("本次初评结果未通过校验，请修正后重新输出完整 JSON：\n");
        for (String problem : problems) {
            builder.append("- ").append(problem).append('\n');
        }
        if (!fixed.isEmpty()) {
            builder.append("（系统已按要点明细自动核算并修正：").append(String.join("；", fixed))
                    .append("，请保持一致）\n");
        }
        return builder.append("要求：各要点满分之和必须等于题目满分，preliminaryScore 必须等于各要点得分之和，")
                .append("awardedScore 必须在 0 到 maxScore 之间，pointNo 从 1 开始连续。\n")
                .append("referencePointCoverage 要求：先逐条列出参考答案中的每一条采分点（题目每个要求部分都要覆盖），")
                .append("再用 coveredByPointNos 标明每条采分点由哪些评分要点覆盖；")
                .append("参考答案标注（可选）的条目 optional 置为 true，且不要为它单独设立要点。")
                .toString();
    }
}
