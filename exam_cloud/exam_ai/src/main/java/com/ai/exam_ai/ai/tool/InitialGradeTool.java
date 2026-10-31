package com.ai.exam_ai.ai.tool;

import com.ai.exam_ai.ai.GradingContext;
import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 初评结果读取工具：供复核 Agent 通过工具获取初评 Agent 产出的结果
 */
@Component
@RequiredArgsConstructor
public class InitialGradeTool {

    private final GradingContext gradingContext;

    /**
     * 获取当前批阅题目的初评结果，由初评 Agent 产出，供复核 Agent 对照裁决
     */
    @Tool("获取当前批阅题目的初评结果，由初评 Agent 产出，供复核 Agent 对照裁决；复核时必须先调用本工具再逐点裁决")
    public InitialGradeResult getInitialGrade() {
        return gradingContext.getInitialGrade();
    }
}
