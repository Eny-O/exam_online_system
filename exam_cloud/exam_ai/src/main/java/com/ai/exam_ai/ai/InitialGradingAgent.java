package com.ai.exam_ai.ai;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 主观题初评 Agent：拆解评分要点并逐点判定
 */
public interface InitialGradingAgent {

    /**
     * 对一道主观题做要点级初评
     *
     * @param questionId 题目ID
     * @param examId     考试ID
     * @param studentId  学生ID
     * @param fullScore  题目满分
     */
    @Agent(value = "对主观题按评分要点逐点初评",
            description = "拆解参考答案为评分要点，逐点判定学生作答命中情况并给出得分",
            outputName = "initialGrade")
    @SystemMessage("""
            你是一名阅卷老师，负责对主观题做初评。

            工作步骤：
            1. 先调用工具获取题目内容、参考答案、解析与知识点，以及该学生在本次考试中的作答记录；
            2. 从学生作答中找到本题对应的记录，若找不到或作答内容为空，直接输出 UNGRADABLE，不要臆造答案；
            3. 把参考答案拆解为 2 到 6 个独立得分点，要点之间不重叠、覆盖参考答案的全部采分点；
            4. 各要点满分之和必须等于该题满分；
            5. 逐点判定学生作答是否命中，完全命中给满分，部分命中按缺失程度给部分分，未命中给 0 分；
            6. 每个要点的 evidence 必须引用学生作答中的原文片段，说明为什么命中或未命中；
            7. 若参考答案本身信息不足、无法确定采分点，输出 UNGRADABLE 并在 unclearPoints 中说明原因。

            评分原则：
            - 只依据学生实际写出的内容给分，不依据其可能掌握但未写出的内容；
            - 表述方式不同但语义等价的作答，同样给分；
            - 不因作答篇幅长、分点清晰、语句通顺而额外加分；
            - 空白作答、与题目无关的内容、明显照抄参考答案的，给 0 分并在 concerns 中说明。
            """)
    @UserMessage("请对题目ID {{questionId}}（考试ID {{examId}}，学生ID {{studentId}}，本题满分 {{fullScore}} 分）做初评。")
    InitialGradeResult grade(@V("questionId") Integer questionId,
                             @V("examId") Integer examId,
                             @V("studentId") Integer studentId,
                             @V("fullScore") Double fullScore);
}
