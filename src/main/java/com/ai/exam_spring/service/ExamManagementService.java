package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.pojo.dto.ExamDetailDTO;
import com.ai.exam_spring.pojo.dto.ExamQueryDTO;
import com.ai.exam_spring.pojo.vo.ExamVO;

public interface ExamManagementService {
    PageResult<ExamVO> pageExam(ExamQueryDTO examQueryDTO);

    Integer addExam(ExamDetailDTO examDetailDTO);

}
