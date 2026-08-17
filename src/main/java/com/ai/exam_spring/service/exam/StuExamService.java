package com.ai.exam_spring.service.exam;

import com.ai.exam_spring.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.pojo.vo.exam.ExamResultVO;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.pojo.vo.stu_answer.ExamAnswerVO;

import java.util.List;
import java.util.Map;

public interface StuExamService {
    /**
     * 根据id获取考试信息
     * @param id
     * @return
     */
    StuExamVO examInfoById(Integer id);

    /**
     * 根据id获取试卷信息
     * @param id
     * @return
     */
    StuPaperVO getPaperById(Integer id);

    /**
     * 提交试卷
     * @param studentUpdateAnswerDTOS
     */
    void submitPaper(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS);

    /**
     * 获取答题详情
     * @param examId
     * @return
     */
    List<BaseStudentAnswerVO> getAnswerDetail(Integer examId);

    /**
     * 获取考试结果
     * @param examId
     * @return
     */
    ExamResultVO getExamResult(Integer examId);

    /**
     * 获取所有考试成绩
     * @return
     */
    List<ExamResultVO> getAllExamResult();

    /**
     * 初始化学生考试数据
     * @param examId
     */
    Map<Integer, Integer> initStuExam(Integer examId);

    /**
     * 批量更新学生答案
     * @param studentUpdateAnswerDTOS
     */
    void updateBatchAnswer(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS);


}
