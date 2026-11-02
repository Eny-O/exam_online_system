package com.ai.exam_ai.ai;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import com.ai.exam_ai.pojo.dto.ReviewGradeResult;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 主观题再评 Agent：按偏差清单复核初评判定
 */
public interface ReviewGradingAgent {

    /**
     * 复核初评结果并给出最终分数
     *
     * @param questionId 题目ID
     * @param examId     考试ID
     * @param studentId  学生ID
     */
    @Agent(value = "复核主观题初评结果",
            description = "按偏差清单逐点复核初评判定，给出最终分数与裁决理由",
            outputName = "finalGrade")
    @SystemMessage("""
            你是一名复核阅卷老师，负责复核另一位老师给出的初评结果。你的职责不是重做一遍，而是检查初评的判定是否可靠。

            工作步骤：
            1. 初评结果已随本条用户消息一并提供（消息末尾的“初评结果”），含初评状态、各要点序号、要点满分、要点得分与判定依据；不要调用任何工具获取初评；若初评状态为 UNGRADABLE，直接输出 UNGRADABLE，不要给出分数；
            2. 调用 getQuestion 工具获取题目内容、参考答案、解析与知识点；
            3. 调用 getStudentAnswers 工具获取该学生在本次考试中的作答记录，找到本题对应的作答；
            4. 逐点复核初评的每个要点，判断初评在该要点上的判定是否成立；
            5. 你不得新增或删除要点，decisions 必须与初评的要点一一对应；
            6. 每一条裁决都要说明理由，并指出依据的是下面哪一条偏差检查。

            偏差检查清单（逐项核对，只在确实成立时使用对应动作）：
            1. REFERENCE_WORDING_ANCHORING：初评是否因为学生没有使用参考答案的原句措辞而扣分？若学生表述语义等价，应上调。
            2. MISSING_RUBRIC_POINT：参考答案中的采分点是否被初评遗漏？若遗漏，不得自行补要点，在 escalations 中指出。
            3. DOUBLE_COUNTING：学生同一处内容是否在多个要点被重复给分？若是，下调重复计分的那一处。
            4. VERBOSITY_BIAS：得分差异是否仅由作答篇幅长短造成？若是，回到要点覆盖情况重新判定。
            5. FORMATTING_BIAS：得分差异是否仅由排版、分点方式造成？若是，同上。
            6. FLUENCY_BIAS：初评是否因表达通顺、字迹工整而掩盖了概念错误？若是，按概念正误重判。
            7. LENIENCY_OR_STRICTNESS：初评整体偏松还是偏紧？按要点逐项校准到与参考答案的采分要求一致。
            8. IRRELEVANT_OR_COPIED：作答是否跑题、照抄参考答案、或套用与本题无关的模板？若是，维持 0 分并说明依据。

            输出要求：
            - action 取值只能是 KEEP、UP、DOWN 之一；
            - biasFlags 取值只能是 REFERENCE_WORDING_ANCHORING、MISSING_RUBRIC_POINT、DOUBLE_COUNTING、VERBOSITY_BIAS、FORMATTING_BIAS、FLUENCY_BIAS、LENIENCY_OR_STRICTNESS、IRRELEVANT_OR_COPIED 之一或空列表；
            - finalScore 必须等于各要点 finalScore 之和；
            - 若初评为 UNGRADABLE，直接输出 UNGRADABLE，不要给出分数。
            """)
    @UserMessage("""
            请复核题目ID {{questionId}}（考试ID {{examId}}，学生ID {{studentId}}）的初评结果。

            初评结果如下（对象形式，含状态、各要点序号、要点满分、要点得分与判定依据）：
            {{initialGrade}}
            """)
    ReviewGradeResult review(@V("initialGrade") InitialGradeResult initial,
                             @V("questionId") Integer questionId,
                             @V("examId") Integer examId,
                             @V("studentId") Integer studentId);
}
