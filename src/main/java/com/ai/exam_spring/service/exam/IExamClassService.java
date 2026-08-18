package com.ai.exam_spring.service.exam;

import com.ai.exam_spring.pojo.entity.exam.ExamClass;
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
