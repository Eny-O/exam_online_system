package com.ai.exam_spring.service.grading;

import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerAddDTO;
import com.ai.exam_spring.pojo.vo.grade.GradePaperContentVO;
import com.ai.exam_spring.pojo.vo.grade.GradeStudentVO;

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
     * 保存批阅学生答案
     * @param studentAnswerAddDTO
     */
    void saveGradeStudentAnswer(StudentAnswerAddDTO studentAnswerAddDTO);
}
