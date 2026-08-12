package com.ai.exam_spring.service.grading;

import com.ai.exam_spring.pojo.vo.grade.GradePaperContentVO;
import com.ai.exam_spring.pojo.vo.grade.GradeStudentVO;
import com.ai.exam_spring.pojo.vo.stu_answer.StudentAnswerVO;

import java.util.List;

public interface GradePaperService {

    /**
     * 获取批阅试卷
     * @param examId
     * @return
     */
    GradePaperContentVO getGradePaper(Integer examId);

    /**
     * 获取学生答题情况
     * @param studentId
     * @return
     */
    List<StudentAnswerVO> getStudentAnswer(Integer studentId, Integer examId);

    /**
     * 获取要批阅学生列表
     * @param examId
     * @return
     */
    List<GradeStudentVO> getGradeStudentList(Integer examId);
}
