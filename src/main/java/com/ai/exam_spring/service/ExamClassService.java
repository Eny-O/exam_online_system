package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.entity.ExamClass;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ExamClassService extends IService<ExamClass> {
    void add(Integer examId, List<Integer> classIds);
}
