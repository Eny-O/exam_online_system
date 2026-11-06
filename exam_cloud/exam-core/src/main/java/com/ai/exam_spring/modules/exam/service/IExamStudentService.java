package com.ai.exam_spring.modules.exam.service;

import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IExamStudentService extends IService<ExamStudent> {
    /**
     * 批量添加考试学生
     * @param examId 考试id
     * @param studentIds 学生id列表
     */
    void add(Integer examId, List<Integer> studentIds);

    /**
     * 强制收卷或判缺考
     * Redis 有暂存答案 → 落库 + 置已交卷 + 触发客观题自动批阅；
     * Redis 无暂存答案 → 判为缺考。
     * 处理失败时抛出异常，由调用方决定兜底策略。
     * @param examId 考试id
     * @param studentId 学生id
     */
    void forceSubmitOrMarkAbsent(Integer examId, Integer studentId);

}
