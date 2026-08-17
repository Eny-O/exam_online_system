package com.ai.exam_spring.service.grading;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import com.ai.exam_spring.pojo.vo.grade.GradePaperContentVO;
import com.ai.exam_spring.pojo.vo.grade.GradeStudentVO;
import com.ai.exam_spring.pojo.vo.stu_answer.ExamAnswerVO;
import org.springframework.transaction.annotation.Transactional;

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
