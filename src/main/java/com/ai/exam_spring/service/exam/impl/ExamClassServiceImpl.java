package com.ai.exam_spring.service.exam.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.pojo.entity.exam.ExamClass;
import com.ai.exam_spring.mapper.ExamClassMapper;
import com.ai.exam_spring.service.exam.IExamClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamClassServiceImpl extends ServiceImpl<ExamClassMapper, ExamClass> implements IExamClassService {

    /**
     * 添加试卷与n个班级的关联信息
     * @param examId 考试id
     * @param classIds 班级id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer examId, List<Integer> classIds) {
        if (examId == null || classIds == null || classIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId));
        List<ExamClass> examClassList = classIds.stream()
                .distinct()
                .map(classId -> new ExamClass(examId, classId))
                .toList();
        saveBatch(examClassList);
    }
}
