package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.RedisAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.utils.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考生答案落库服务：把 Redis 中暂存的答案搬到数据库。
 * <p>
 * 手动交卷（{@code StuExamServiceImpl#submitPaper}）与考试到点的定时自动交卷
 * （{@code ExamAutoSubmitTask}）共用这一份实现，避免两条链路逻辑漂移：
 * 分数、时间字段填充、幂等删除策略只需维护一处。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamAnswerPersistenceService {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamRuntimePaperService examRuntimePaperService;

    /**
     * 读取某考生在某场考试下暂存于 Redis 的答案
     *
     * @param examId    考试ID
     * @param studentId 学生ID
     * @return 答案实体列表，无暂存答案时返回空列表
     */
    public List<StudentAnswer> loadAnswersFromRedis(Integer examId, Integer studentId) {
        String redisKey = RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + studentId;
        Map<String, String> rawAnswerMap = redisUtil.hGetAllRaw(redisKey);
        if (rawAnswerMap.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        List<StudentAnswer> studentAnswers = new ArrayList<>(rawAnswerMap.size());
        for (Map.Entry<String, String> entry : rawAnswerMap.entrySet()) {
            Integer questionId;
            try {
                questionId = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                log.warn("Redis 答案 field 不是合法题目ID，已跳过。examId={} studentId={} field={}",
                        examId, studentId, entry.getKey());
                continue;
            }
            RedisAnswerDTO redisAnswer;
            try {
                redisAnswer = objectMapper.readValue(entry.getValue(), RedisAnswerDTO.class);
            } catch (Exception e) {
                log.error("Redis 答案解析失败，已跳过该题。examId={} studentId={} questionId={} raw={}",
                        examId, studentId, questionId, entry.getValue(), e);
                continue;
            }
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setExamId(examId);
            studentAnswer.setStudentId(studentId);
            studentAnswer.setQuestionId(questionId);
            studentAnswer.setStudentAnswer(redisAnswer.getStudentAnswer());
            studentAnswer.setFullScore(redisAnswer.getFullScore());
            // batchInsert 是手写 XML，不经过 MyBatis-Plus 的自动填充，时间需显式赋值，
            // 否则插入的是 NULL（列上声明了 DEFAULT CURRENT_TIMESTAMP 也不会生效）
            studentAnswer.setCreateTime(now);
            studentAnswer.setUpdateTime(now);
            studentAnswers.add(studentAnswer);
        }
        return studentAnswers;
    }

    /**
     * 将答案写入数据库（先删后插，保证幂等）
     * <p>
     * student_answer 表没有 (exam_id, student_id, question_id) 唯一键，
     * 定时交卷是周期性执行的，若不先清理同一考生的旧记录会产生重复行、
     * 导致批阅时重复计分，故此处先按 (exam_id, student_id) 删除再批量插入。
     *
     * @param studentAnswers 答案实体列表
     * @return 实际写入的答案条数
     */
    public int saveAnswers(List<StudentAnswer> studentAnswers) {
        if (studentAnswers == null || studentAnswers.isEmpty()) {
            return 0;
        }
        Integer examId = studentAnswers.get(0).getExamId();
        Integer studentId = studentAnswers.get(0).getStudentId();
        Map<Integer, String> questionTypeMap = examRuntimePaperService.getQuestionTypeMap(examId);
        for (StudentAnswer studentAnswer : studentAnswers) {
            studentAnswer.setQuestionType(questionTypeMap.get(studentAnswer.getQuestionId()));
        }
        studentAnswerMapper.deleteByExamAndStudent(examId, studentId);
        studentAnswerMapper.batchInsert(studentAnswers);
        log.info("答案落库完成：examId={} studentId={} 条数={}", examId, studentId, studentAnswers.size());
        return studentAnswers.size();
    }

    /**
     * 读取 Redis 中暂存的答案并落库
     *
     * @param examId    考试ID
     * @param studentId 学生ID
     * @return 实际写入的答案条数，无暂存答案时返回 0
     */
    public int persistAnswersFromRedis(Integer examId, Integer studentId) {
        List<StudentAnswer> studentAnswers = loadAnswersFromRedis(examId, studentId);
        if (studentAnswers.isEmpty()) {
            return 0;
        }
        return saveAnswers(studentAnswers);
    }
}
