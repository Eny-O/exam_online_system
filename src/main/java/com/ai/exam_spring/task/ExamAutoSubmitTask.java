package com.ai.exam_spring.task;


import com.ai.exam_spring.pojo.dto.exam.ExamStatusUpdateDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.entity.exam.ExamStudent;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import com.ai.exam_spring.pojo.enums.exam.StudentExamEnum;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.IExamStudentService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExamAutoSubmitTask {

    @Autowired
    private IExamManagementService examManagementService;
    @Autowired
    private IExamStudentService examStudentService;
    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Scheduled(fixedRate = 60000)
    public void autoSubmitExpiredExams() {
        // 查询考试超时的列表
        LocalDateTime now = LocalDateTime.now();
        List<Exam> expiredExams = examManagementService.list(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, ExamEnum.ALREADY_POST)
                .apply("DATE_ADD(start_time, INTERVAL duration MINUTE) < {0}", now));
        if (expiredExams.isEmpty()) {
            return;
        }
        expiredExams.forEach(expiredExam -> {
            Integer examId = expiredExam.getId();
            List<ExamStudent> dbExamPendingStudents = examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                    .eq(ExamStudent::getExamId, examId)
                    .eq(ExamStudent::getStatus, StudentExamEnum.PENDING));
            List<Integer> pendingStudentIds = dbExamPendingStudents.stream().map(ExamStudent::getStudentId).toList();
            if (pendingStudentIds.isEmpty()) {
                // 没有 pending 学生，仅流转考试状态
                examManagementService.updateExamStatus(new ExamStatusUpdateDTO(examId, ExamEnum.PENDING_REVIEW));
                return;
            }
            List<StudentAnswer> dbPendingStudentAnswers = studentAnswerService.list(new LambdaQueryWrapper<StudentAnswer>()
                    .eq(StudentAnswer::getExamId, examId)
                    .in(StudentAnswer::getStudentId, pendingStudentIds));
            Set<Integer> haveAnswerSet = new HashSet<>(
                    dbPendingStudentAnswers.stream().map(StudentAnswer::getStudentId).toList());
            // 无答题记录 → 缺考
            List<Integer> absentStudentIds = dbExamPendingStudents.stream()
                    .map(ExamStudent::getStudentId)
                    .filter(studentId -> !haveAnswerSet.contains(studentId))
                    .toList();
            // 有答题记录 → 已提交
            List<Integer> submittedStudentIds = dbExamPendingStudents.stream()
                    .map(ExamStudent::getStudentId)
                    .filter(haveAnswerSet::contains)
                    .toList();
            if (!absentStudentIds.isEmpty()) {
                examStudentService.lambdaUpdate()
                        .eq(ExamStudent::getExamId, examId)
                        .in(ExamStudent::getStudentId, absentStudentIds)
                        .set(ExamStudent::getStatus, StudentExamEnum.ABSENT)
                        .update();
            }
            if (!submittedStudentIds.isEmpty()) {
                examStudentService.lambdaUpdate()
                        .eq(ExamStudent::getExamId, examId)
                        .in(ExamStudent::getStudentId, submittedStudentIds)
                        .set(ExamStudent::getStatus, StudentExamEnum.SUBMITTED)
                        .set(ExamStudent::getSubmitTime, LocalDateTime.now())
                        .update();
            }
            examManagementService.updateExamStatus(new ExamStatusUpdateDTO(examId, ExamEnum.PENDING_REVIEW));
        });
    }

}
