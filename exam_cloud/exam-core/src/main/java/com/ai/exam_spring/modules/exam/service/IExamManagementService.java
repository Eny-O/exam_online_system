package com.ai.exam_spring.modules.exam.service;

import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dto.*;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamDetailVO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
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

    /**
     * 将考试信息与试卷内容加载到redis缓存
     * @param id 考试ID
     */
    void publishExam(Integer id);

    /**
     * 清除考试在redis中的缓存
     * @param id 考试ID
     */
    void clearExamCache(Integer id);
}
