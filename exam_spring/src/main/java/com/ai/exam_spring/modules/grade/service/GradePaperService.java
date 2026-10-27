package com.ai.exam_spring.modules.grade.service;

import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradeStudentVO;

import java.util.List;

public interface GradePaperService {

    /**
     * 获取批阅试卷
     * @param examId
     * @return
     */
    GradePaperContentVO getGradePaper(Integer examId);


    /**
     * 获取要批阅学生列表
     * @param examId
     * @return
     */
    List<GradeStudentVO> getGradeStudentList(Integer examId);


    /**
     * 自动批改客观题
     * @param examId
     */
    void autoGradeObjectiveQuestions(Integer examId, Integer studentId);

}
