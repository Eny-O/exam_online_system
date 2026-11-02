package com.ai.exam_ai.ai;

import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 主观题双评批阅编排：
 * 由 GradingAgentConfig 装配为顺序工作流（初评 Agent -> 复核 Agent），
 * 初评结果经 AgenticScope 的 initialGrade 变量由框架自动注入复核 Agent（@V）。
 *
 * 输入经 @V 变量注入各子 Agent 的消息模板，不再额外发送顶层消息。
 */
public interface SubjectiveGradingWorkflow {

    @Agent(value = "主观题双评批阅",
            description = "先完成初评，再按偏差清单复核初评判定，给出最终分数与裁决理由")
    ReviewGradeResult grade(@V("examId") Integer examId,
                            @V("studentId") Integer studentId,
                            @V("questionId") Integer questionId,
                            @V("fullScore") Double fullScore);
}
