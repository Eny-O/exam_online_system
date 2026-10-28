package com.ai.exam_spring.modules.exam.service;

import com.ai.exam_spring.modules.exam.pojo.dos.ExamClass;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IExamClassService extends IService<ExamClass> {
    /**
     * 添加试卷与n个班级的关联信息
     * @param examId
     * @param classIds
     */
    void add(Integer examId, List<Integer> classIds);
}
