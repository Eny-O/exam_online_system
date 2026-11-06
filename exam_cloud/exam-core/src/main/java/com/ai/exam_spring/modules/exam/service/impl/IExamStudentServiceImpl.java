package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_spring.modules.exam.mapper.ExamStudentMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class IExamStudentServiceImpl extends ServiceImpl<ExamStudentMapper, ExamStudent> implements IExamStudentService {

    @Autowired
    private ExamAnswerPersistenceService examAnswerPersistenceService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 添加考试学生关系
     * @param examId
     * @param studentIds
     */
    @Override
    public void add(Integer examId, List<Integer> studentIds) {
        if (examId == null || studentIds == null || studentIds.isEmpty()){
            return;
        }
        remove(new LambdaQueryWrapper<ExamStudent>().eq(ExamStudent::getExamId, examId));
        List<ExamStudent> examStudents = studentIds.stream()
                .distinct()
                .map(studentId -> new ExamStudent(examId, studentId)).toList();
        saveBatch(examStudents);
    }

    /**
     * 强制收卷或判缺考
     * Redis 有暂存答案 → 落库 + 置已交卷 + 触发客观题自动批阅；
     * Redis 无暂存答案 → 判为缺考。
     * 处理失败时抛出异常，由调用方决定兜底策略。
     * @param examId 考试id
     * @param studentId 学生id
     */
    @Override
    public void forceSubmitOrMarkAbsent(Integer examId, Integer studentId) {
        List<StudentAnswer> studentAnswers = examAnswerPersistenceService.loadAnswersFromRedis(examId, studentId);
        if (studentAnswers.isEmpty()) {
            // 无答题记录 → 缺考
            lambdaUpdate()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStudentId, studentId)
                    .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)
                    .set(ExamStudent::getStatus, StudentExamEnum.ABSENT)
                    .update();
            log.info("强制收卷：无作答记录，判为缺考。examId={} studentId={}", examId, studentId);
            return;
        }
        // 有答题记录 → 落库并置为已交卷
        examAnswerPersistenceService.saveAnswers(studentAnswers);
        boolean updated = lambdaUpdate()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, studentId)
                .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)
                .set(ExamStudent::getStatus, StudentExamEnum.SUBMITTED)
                .set(ExamStudent::getSubmitTime, LocalDateTime.now())
                .update();
        if (!updated) {
            // 状态已被其它链路改动（如学生手动交卷），不重复触发批阅
            log.info("强制收卷：考生状态已变更，跳过批阅触发。examId={} studentId={}", examId, studentId);
            return;
        }
        // 触发客观题自动批阅
        rocketMQTemplate.convertAndSend("exam-grade-topic", new GradeMessageDTO(examId, studentId));
        // 落库成功后清理 Redis 中的暂存答案，避免脏数据长期堆积
        redisUtil.delete(RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + studentId);
        log.info("强制收卷：答案已落库。examId={} studentId={} 题数={}", examId, studentId, studentAnswers.size());
    }
}
