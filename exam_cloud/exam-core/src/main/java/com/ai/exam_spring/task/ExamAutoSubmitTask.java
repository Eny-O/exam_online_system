package com.ai.exam_spring.task;

import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamStatusUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.service.impl.ExamAnswerPersistenceService;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试到点自动交卷定时任务。
 * <p>
 * 职责：考试时长结束后，把考生在 Redis 中暂存的答案落库、置为已交卷并触发客观题自动批阅；
 * 没有任何作答记录的考生判为缺考；最后把考试流转到待批阅。
 * <p>
 * 说明：答案落库逻辑与手动交卷共用 {@link ExamAnswerPersistenceService}，
 * 保证两条链路的时间字段填充与幂等删除策略一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamAutoSubmitTask {

    private final IExamManagementService examManagementService;
    private final IExamStudentService examStudentService;
    private final ExamAnswerPersistenceService examAnswerPersistenceService;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisUtil redisUtil;

    /**
     * 每分钟扫描一次已到结束时间的考试并自动交卷。
     * <p>
     * 幂等性由三层保证：只捞 status=PENDING 的考生；落库前先删该考生旧答案再插入；
     * 更新考生状态时带 status=PENDING 条件，已处理的记录更新不到、不会重复触发。
     */
    @Scheduled(fixedRate = 60000)
    public void autoSubmitExpiredExams() {
        LocalDateTime now = LocalDateTime.now();
        // 查询考试超时的列表
        List<Exam> expiredExams = examManagementService.list(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, ExamEnum.ALREADY_POST)
                .apply("DATE_ADD(start_time, INTERVAL duration MINUTE) < {0}", now));
        if (expiredExams.isEmpty()) {
            return;
        }
        log.info("自动交卷定时任务启动：超时考试数={}", expiredExams.size());
        expiredExams.forEach(this::handleExpiredExam);
    }

    /**
     * 处理一场超时考试：逐个考生自动交卷，最后流转考试状态
     */
    private void handleExpiredExam(Exam expiredExam) {
        Integer examId = expiredExam.getId();
        try {
            List<ExamStudent> pendingStudents = examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStatus, StudentExamEnum.PENDING));
            for (ExamStudent examStudent : pendingStudents) {
                try {
                    autoSubmitStudent(examId, examStudent.getStudentId());
                } catch (Exception e) {
                    // 单个考生失败不影响整场考试：下一轮任务会再次捞起该考生（状态仍为 PENDING）重试
                    log.error("自动交卷失败，保留 Redis 答案待下轮重试。examId={} studentId={}",
                            examId, examStudent.getStudentId(), e);
                }
            }
            // 无 pending 学生时同样需要流转考试状态
            examManagementService.updateExamStatus(new ExamStatusUpdateDTO(examId, ExamEnum.PENDING_REVIEW));
            log.info("自动交卷完成，考试进入待批阅。examId={}", examId);
        } catch (Exception e) {
            log.error("自动交卷处理考试失败。examId={}", examId, e);
        }
    }

    /**
     * 为单个考生自动交卷：
     * Redis 有暂存答案 → 落库 + 置已交卷 + 触发客观题自动批阅；
     * Redis 无暂存答案 → 判为缺考。
     */
    private void autoSubmitStudent(Integer examId, Integer studentId) {
        List<StudentAnswer> studentAnswers = examAnswerPersistenceService.loadAnswersFromRedis(examId, studentId);
        if (studentAnswers.isEmpty()) {
            // 无答题记录 → 缺考
            examStudentService.lambdaUpdate()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStudentId, studentId)
                    .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)
                    .set(ExamStudent::getStatus, StudentExamEnum.ABSENT)
                    .update();
            log.info("自动交卷：无作答记录，判为缺考。examId={} studentId={}", examId, studentId);
            return;
        }
        // 有答题记录 → 落库并置为已交卷
        examAnswerPersistenceService.saveAnswers(studentAnswers);
        boolean updated = examStudentService.lambdaUpdate()
                .eq(ExamStudent::getExamId, examId)
                .eq(ExamStudent::getStudentId, studentId)
                .eq(ExamStudent::getStatus, StudentExamEnum.PENDING)
                .set(ExamStudent::getStatus, StudentExamEnum.SUBMITTED)
                .set(ExamStudent::getSubmitTime, LocalDateTime.now())
                .update();
        if (!updated) {
            // 状态已被其它链路改动（如学生手动交卷），不重复触发批阅
            log.info("自动交卷：考生状态已变更，跳过批阅触发。examId={} studentId={}", examId, studentId);
            return;
        }
        // 触发客观题自动批阅
        rocketMQTemplate.convertAndSend("exam-grade-topic", new GradeMessageDTO(examId, studentId));
        // 落库成功后清理 Redis 中的暂存答案，避免脏数据长期堆积
        redisUtil.delete(RedisConstant.EXAM_ANSWER_PREFIX + examId + ":" + studentId);
        log.info("自动交卷：答案已落库。examId={} studentId={} 题数={}", examId, studentId, studentAnswers.size());
    }
}
