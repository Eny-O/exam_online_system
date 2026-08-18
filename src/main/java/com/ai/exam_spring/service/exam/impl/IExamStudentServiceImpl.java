package com.ai.exam_spring.service.exam.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.mapper.ExamStudentMapper;
import com.ai.exam_spring.pojo.entity.exam.ExamStudent;
import com.ai.exam_spring.service.exam.IExamStudentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IExamStudentServiceImpl extends ServiceImpl<ExamStudentMapper, ExamStudent> implements IExamStudentService {

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
}
