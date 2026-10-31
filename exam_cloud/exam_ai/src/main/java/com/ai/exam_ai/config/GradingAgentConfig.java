package com.ai.exam_ai.config;

import com.ai.exam_ai.ai.GradingContext;
import com.ai.exam_ai.ai.InitialGradingAgent;
import com.ai.exam_ai.ai.ReviewGradingAgent;
import com.ai.exam_ai.ai.SubjectiveGradingWorkflow;
import com.ai.exam_ai.ai.tool.GradingTool;
import com.ai.exam_ai.ai.tool.InitialGradeTool;
import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 主观题批阅 Agent 装配
 */
@Configuration
@RequiredArgsConstructor
public class GradingAgentConfig {

    private final ChatModel chatModel;
    private final GradingTool gradingTool;
    private final InitialGradeTool initialGradeTool;
    private final GradingContext gradingContext;

    /**
     * 主观题双评顺序工作流：初评 → 再评（再评经工具读取初评）。
     * 初评 Agent 产出写入 AgenticScope 的 initialGrade，beforeCall 桥接到 GradingContext，
     * 供再评 Agent 通过 getInitialGrade 工具读取。
     */
    @Bean
    public SubjectiveGradingWorkflow subjectiveGradingWorkflow() {
        InitialGradingAgent initialGradingAgent = AgenticServices.agentBuilder(InitialGradingAgent.class)
                .chatModel(chatModel)
                .tools(gradingTool)
                .maxSequentialToolsInvocations(3)
                .build();

        ReviewGradingAgent reviewGradingAgent = AgenticServices.agentBuilder(ReviewGradingAgent.class)
                .chatModel(chatModel)
                .tools(gradingTool, initialGradeTool)
                .maxSequentialToolsInvocations(5)
                .build();

        return AgenticServices.sequenceBuilder(SubjectiveGradingWorkflow.class)
                .subAgents(initialGradingAgent, reviewGradingAgent)
                .outputName("finalGrade")
                .beforeCall((AgenticScope scope) -> {
                    Object raw = scope.hasState("initialGrade") ? scope.readState("initialGrade") : null;
                    if (raw == null) {
                        for (Object value : scope.state().values()) {
                            if (value instanceof InitialGradeResult) {
                                raw = value;
                                break;
                            }
                        }
                    }
                    if (raw instanceof InitialGradeResult initialGrade) {
                        gradingContext.putInitialGrade(initialGrade);
                    }
                })
                .build();
    }
}
