package com.ai.exam_ai.config;

import com.ai.exam_ai.ai.GradingContext;
import com.ai.exam_ai.ai.InitialGradingAgent;
import com.ai.exam_ai.ai.ReviewGradingAgent;
import com.ai.exam_ai.ai.SubjectiveGradingWorkflow;
import com.ai.exam_ai.ai.tool.GradingTool;
import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 主观题批阅 Agent 装配：
 * - 初评 / 复核两个独立 Agent；
 * - 顺序工作流（gradingSequence）：用 sequenceBuilder(Class) 装配为强类型的 SubjectiveGradingWorkflow，
 *   初评结果经 AgenticScope 的 initialGrade 变量由框架自动注入复核 Agent（@V）。
 */
@Configuration
@RequiredArgsConstructor
public class GradingAgentConfig {

    private final ChatModel chatModel;
    private final GradingTool gradingTool;
    private final GradingContext gradingContext;

    @Bean
    public InitialGradingAgent initialGradingAgent() {
        return AgenticServices.agentBuilder(InitialGradingAgent.class)
                .chatModel(chatModel)
                .tools(gradingTool)
                .maxSequentialToolsInvocations(3)
                .build();
    }

    @Bean
    public ReviewGradingAgent reviewGradingAgent() {
        return AgenticServices.agentBuilder(ReviewGradingAgent.class)
                .chatModel(chatModel)
                .tools(gradingTool)
                .maxSequentialToolsInvocations(5)
                .build();
    }

    /** 顺序工作流：初评 -> 复核；装配为强类型的 SubjectiveGradingWorkflow，业务侧按接口直接调用 */
    @Bean
    public SubjectiveGradingWorkflow gradingSequence(InitialGradingAgent initialGradingAgent,
                                                     ReviewGradingAgent reviewGradingAgent) {
        return AgenticServices.sequenceBuilder(SubjectiveGradingWorkflow.class)
                .subAgents(initialGradingAgent, reviewGradingAgent)
                .outputName("finalGrade")
                .output(this::syncInitialGrade)
                .build();
    }

    /** 工作流结束时把初评结果回写 GradingContext，保持服务层读取初评的原有方式不变 */
    private Object syncInitialGrade(AgenticScope scope) {
        InitialGradeResult initial = (InitialGradeResult) scope.readState("initialGrade");
        if (initial != null) {
            gradingContext.putInitialGrade(initial);
        }
        return scope.readState("finalGrade");
    }
}
