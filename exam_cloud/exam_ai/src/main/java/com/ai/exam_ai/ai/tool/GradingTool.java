package com.ai.exam_ai.ai.tool;

import com.ai.exam_api.pojo.dto.QuestionBriefDTO;
import com.ai.exam_api.pojo.dto.StudentAnswerDTO;
import com.ai.exam_api.feign.QuestionFeignClient;
import com.ai.exam_api.feign.StudentAnswerFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批阅取数工具：向 exam-core 获取题目与学生作答
 */
@Component
@RequiredArgsConstructor
public class GradingTool {

    private final QuestionFeignClient questionFeignClient;
    private final StudentAnswerFeignClient studentAnswerFeignClient;

    /**
     * 获取题目内容、参考答案、解析与知识点
     *
     * @param questionId 题目ID
     */
    @Tool("获取指定题目的题干、参考答案、解析、知识点与题型")
    public QuestionBriefDTO getQuestion(@P("题目ID") Integer questionId) {
        List<QuestionBriefDTO> questions = questionFeignClient.listByIds(List.of(questionId)).getData();
        return questions.isEmpty() ? null : questions.get(0);
    }

    /**
     * 获取某次考试中某位学生的全部作答
     *
     * @param examId    考试ID
     * @param studentId 学生ID
     */
    @Tool("获取某次考试中某位学生的全部作答记录，包含题目ID、作答内容与该题满分")
    public List<StudentAnswerDTO> getStudentAnswers(@P("考试ID") Integer examId,
                                                    @P("学生ID") Integer studentId) {
        return studentAnswerFeignClient.listByExamAndStudent(examId, studentId).getData();
    }
}
