package com.ai.exam_spring.service.grading;

import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerUpdateDTO;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.vo.stu_answer.StudentAnswerVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IStudentAnswerService extends IService<StudentAnswer> {
//    /**
//     * 批量保存学生答案
//     * @param studentUpdateAnswerDTO
//     */
//    Boolean batchSaveStudentAnswers(StudentUpdateAnswerDTO studentUpdateAnswerDTO);

    /**
     * 获取学生答题情况
     * @param studentId
     * @return
     */
    List<StudentAnswerVO> getStudentAnswer(Integer studentId, Integer examId);

    /**
     * 更新批阅的学生答案
     * @param studentAnswerUpdateDTOS
     */
    void updateScoreAndGrade(List<StudentAnswerUpdateDTO> studentAnswerUpdateDTOS);
}
