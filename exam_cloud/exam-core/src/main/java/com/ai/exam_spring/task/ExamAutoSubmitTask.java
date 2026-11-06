package com.ai.exam_spring.task;

import com.ai.exam_spring.modules.exam.pojo.dto.ExamStatusUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试到点自动交卷定时任务。
 * <p>
 * 职责：考试时长结束后，把未交卷考生强制收卷或判缺考；最后把考试流转到待批阅。
 * <p>
 * 说明：强制收卷逻辑复用 {@link IExamStudentService#forceSubmitOrMarkAbsent}，
 * 与老师主动结束考试共用同一份收尾实现，避免逻辑分叉。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamAutoSubmitTask {

    private final IExamManagementService examManagementService;
    private final IExamStudentService examStudentService;

    /**
     * 每分钟扫描一次已到结束时间的考试并自动交卷。
     * <p>
     * 幂等性由三层保证：只捞 status=PENDING 的考生；收尾逻辑落库前先删旧答案再插入；
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
     * 处理一场超时考试：逐个考生强制收卷，最后流转考试状态
     */
    private void handleExpiredExam(Exam expiredExam) {
        Integer examId = expiredExam.getId();
        try {
            List<ExamStudent> pendingStudents = examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStatus, StudentExamEnum.PENDING));
            for (ExamStudent examStudent : pendingStudents) {
                try {
                    examStudentService.forceSubmitOrMarkAbsent(examId, examStudent.getStudentId());
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
}
