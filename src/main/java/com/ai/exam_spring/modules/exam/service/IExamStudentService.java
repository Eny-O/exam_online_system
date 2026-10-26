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

}
