package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.pojo.dto.exam.ExamDetailDTO;
import com.ai.exam_spring.pojo.dto.exam.ExamQueryDTO;
import com.ai.exam_spring.pojo.dto.exam.ExamStatusUpdateDTO;
import com.ai.exam_spring.pojo.dto.exam.StuExamQueryDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.vo.exam.ExamVO;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IExamManagementService extends IService<Exam> {
    /**
     * 考试条件分页查询
     * @param examQueryDTO
     * @return
     */
    PageResult<ExamVO> pageExam(ExamQueryDTO examQueryDTO);

    /**
     * 添加考试
     * @param examDetailDTO
     * @return
     */
    Integer addExam(ExamDetailDTO examDetailDTO);

    /**
     * 更新试卷状态
     * @param examStatusUpdateDTO
     */
    void updateExamStatus(ExamStatusUpdateDTO examStatusUpdateDTO);

    /**
     * 学生考试分页条件查询
     * @param stuExamQueryDTO
     * @return PageResult<StuExamVO>
     */
    PageResult<StuExamVO> pageStuExam(StuExamQueryDTO stuExamQueryDTO);
}
