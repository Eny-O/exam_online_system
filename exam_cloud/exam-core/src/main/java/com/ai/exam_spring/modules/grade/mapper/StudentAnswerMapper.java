package com.ai.exam_spring.modules.grade.mapper;

import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.grade.pojo.vo.StudentScoreSummaryVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentAnswerMapper extends BaseMapper<StudentAnswer> {

    /**
     * CASE WHEN 批量更新评分（1条SQL完成多条记录更新）
     * @param gradeList 评分数据列表
     */
    void batchUpdateScore(@Param("gradeList") List<BatchGradeDTO> gradeList);

    /**
     * 多值批量插入（1条SQL插入多条记录）
     * @param list 学生答案列表
     */
    void batchInsert(@Param("list") List<StudentAnswer> list);

    /**
     * 删除某考生在某场考试下的全部答案
     * 用于落库前清理旧记录，保证定时交卷重复执行时不会产生重复行
     * @param examId    考试ID
     * @param studentId 学生ID
     */
    void deleteByExamAndStudent(@Param("examId") Integer examId, @Param("studentId") Integer studentId);

    /**
     * 聚合某场考试下指定学生的客观分、主观分、总分
     * @param examId     考试ID
     * @param studentIds 学生ID列表
     */
    List<StudentScoreSummaryVO> selectScoreSummary(@Param("examId") Integer examId, @Param("studentIds") List<Integer> studentIds);
}
