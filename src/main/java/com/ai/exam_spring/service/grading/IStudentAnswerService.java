package com.ai.exam_spring.service.grading;

import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerDTO;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IStudentAnswerService extends IService<StudentAnswer> {
    /**
     * 批量保存学生答案
     * @param studentAnswerDTO
     */
    void batchSaveStudentAnswers(StudentAnswerDTO studentAnswerDTO);

}
