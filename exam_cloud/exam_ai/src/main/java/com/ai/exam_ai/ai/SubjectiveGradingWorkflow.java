package com.ai.exam_ai.ai;

import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * 主观题批阅顺序工作流
 */
public interface SubjectiveGradingWorkflow {

    /**
     * 执行初评与再评，返回最终批阅结果
     *
     * @param examId     考试ID
     * @param studentId  学生ID
     * @param questionId 题目ID
     */
    @Agent(value = "主观题双评批阅", description = "依次执行初评与再评，输出最终批阅结果")
    ReviewGradeResult grade(@V("examId") Integer examId,
                            @V("studentId") Integer studentId,
                            @V("questionId") Integer questionId);
}
