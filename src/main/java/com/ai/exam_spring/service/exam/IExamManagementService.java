package com.ai.exam_spring.service.exam;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.pojo.dto.exam.*;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.vo.exam.ExamDetailVO;
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
     * @param examDetailAddDTO
     * @return
     */
    Integer addExam(ExamDetailAddDTO examDetailAddDTO);

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

    /**
     * 更新考试
     * @param examDetailUpdateDTO
     */
    void updateExam(ExamDetailUpdateDTO examDetailUpdateDTO);

    /**
     * 根据id获取考试详情
     * @param id
     * @return
     */
    ExamDetailVO getExamDetailById(Integer id);
}
