package com.ai.exam_spring.modules.grade.mapper;

import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.ExamAnswerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GradePaperMapper {
    /**
     * 获取考试答案详情
     * @param examId
     * @param currentUserId
     * @return
     */
    List<ExamAnswerVO> getExamAnswerDetail(Integer examId, @Param("studentId") Integer currentUserId);
}
