package com.ai.exam_spring.mapper;

import com.ai.exam_spring.pojo.vo.exam.ExamResultVO;
import com.ai.exam_spring.pojo.vo.stu_answer.ExamAnswerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StuExamMapper {
    /**
     * 获取所有考试结果
     * @param currentUserId
     * @return
     */
    List<ExamResultVO> getAllExamResult(@Param("studentId") Integer currentUserId);

}
