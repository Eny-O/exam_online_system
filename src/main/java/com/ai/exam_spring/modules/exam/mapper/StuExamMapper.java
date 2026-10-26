package com.ai.exam_spring.modules.exam.mapper;

import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
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
