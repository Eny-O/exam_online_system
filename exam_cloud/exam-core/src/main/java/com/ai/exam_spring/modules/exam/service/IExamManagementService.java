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
     * 发布考试：生成试卷快照、加载卷面缓存并置为已发布
     * @param id 考试ID
     */
    void publishExam(Integer id);

    /**
     * 取消发布：清理卷面缓存、暂存答案与试卷快照并置回未发布
     * @param id 考试ID
     */
    void clearExamCache(Integer id);

    /**
     * 删除考试（未发布物理删除，批阅完成逻辑删除）
     * @param id 考试ID
     */
    void deleteExam(Integer id);
}
