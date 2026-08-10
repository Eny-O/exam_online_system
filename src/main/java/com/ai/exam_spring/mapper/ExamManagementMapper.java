package com.ai.exam_spring.mapper;

import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import com.ai.exam_spring.pojo.vo.exam.ExamVO;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ExamManagementMapper extends BaseMapper<Exam> {
    Page<ExamVO> pageExam(Page<ExamVO> page, @Param("ew") Wrapper<Exam> wrapper, @Param("viewStatus") ExamEnum viewStatus, @Param("now")LocalDateTime now);

    Page<StuExamVO> pageStuExam(Page<Exam> examPage, @Param("ew") Wrapper<Exam> wrapper, @Param("stuId") Integer stuId, @Param("viewStatus") ExamEnum viewStatus, @Param("now")LocalDateTime now);
}
