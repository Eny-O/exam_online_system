package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.domain.entity.ExamClass;
import com.ai.exam_spring.mapper.ExamClassMapper;
import com.ai.exam_spring.service.ExamClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamClassServiceImpl extends ServiceImpl<ExamClassMapper, ExamClass> implements ExamClassService {

    @Autowired
    private ExamClassMapper examClassMapper;

    /**
     * 批量添加考试班级关系
     * @param examId 考试id
     * @param classIds 班级id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(Integer examId, List<Integer> classIds) {
        if (examId == null || classIds == null || classIds.isEmpty()) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        this.remove(new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId));
        List<ExamClass> examClassList = classIds.stream()
                .distinct()
                .map(classId -> new ExamClass(examId, classId))
                .toList();
        saveBatch(examClassList);
    }
}
