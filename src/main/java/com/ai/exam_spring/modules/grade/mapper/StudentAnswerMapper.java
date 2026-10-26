package com.ai.exam_spring.modules.grade.mapper;

import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
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
}
