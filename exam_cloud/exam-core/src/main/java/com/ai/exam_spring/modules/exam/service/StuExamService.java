package com.ai.exam_spring.modules.exam.service;

import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;

import java.util.List;

public interface StuExamService {
    /**
     * 根据id获取考试信息
     * @param id
     * @return
     */
    StuExamVO examInfoById(Integer id);

    /**
     * 获取某场考试的考试信息与试卷内容
     * @param examId 考试ID
     * @return 考试信息与试卷内容
     */
    StuExamPaperVO getExamPaperWithExam(Integer examId);

    /**
     * 根据试卷ID获取试卷内容
     * @param paperId 试卷ID
     * @return StuPaperVO
     */
    StuPaperVO getPaperById(Integer paperId);

    /**
     * 提交试卷
     * 从Redis读取暂存答案批量写入数据库，更新考试状态为已提交，并自动批阅客观题
     * @param examId 考试ID
     */
    void submitPaper(Integer examId);

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
     * 初始化学生考试（仅校验考试和学生信息，不再初始化答案数据）
     * @param examId
     */
    void initStuExam(Integer examId);

    /**
     * 批量更新学生答案
     * @param studentUpdateAnswerDTOS
     */
    void updateBatchAnswer(List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS);


}
