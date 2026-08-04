package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.dto.ExamDetailDTO;
import com.ai.exam_spring.domain.dto.ExamQueryDTO;
import com.ai.exam_spring.domain.entity.Exam;
import com.ai.exam_spring.domain.vo.ExamVO;
import com.ai.exam_spring.mapper.ExamManagementMapper;
import com.ai.exam_spring.service.ExamClassService;
import com.ai.exam_spring.service.ExamManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ExamManagementServiceImpl implements ExamManagementService {

    @Autowired
    private ExamManagementMapper examManagementMapper;

    @Autowired
    private ExamClassService examClassService;

    @Override
    public PageResult<ExamVO> pageExam(ExamQueryDTO examQueryDTO) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        Page<ExamVO> page  = Page.of(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        Wrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(examQueryDTO.getExamName() != null && !examQueryDTO.getExamName().isEmpty(),
                      Exam::getExamName, examQueryDTO.getExamName())
                .apply("exam.creator_id = {0}",currentUserId);
        LocalDateTime now = LocalDateTime.now();
        Page<ExamVO> pageResult = examManagementMapper.pageExam(page, wrapper,  examQueryDTO.getStatus(),now);

        return PageResult.of(pageResult.getTotal(), pageResult.getRecords(), examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
    }

    /**
     * 添加考试
     * @param examDetailDTO
     * @return
     */
    @Override
    public Integer addExam(ExamDetailDTO examDetailDTO) {
        if (examDetailDTO == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        Exam exam = new Exam();
        BeanUtil.copyProperties(examDetailDTO, exam);
        examManagementMapper.insert(exam);
        examClassService.add(exam.getId(),examDetailDTO.getClassIds());
        return exam.getId();
    }
}
