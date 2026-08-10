package com.ai.exam_spring.controller.student;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.exam.StuExamQueryDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.service.IExamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学生考试相关接口")
@RestController
@RequestMapping("/stu/exam")
@Slf4j
public class ExamController {

    @Autowired
    IExamManagementService examManagementService;

    @Operation(description = "学生考试分页条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<StuExamVO>> page(StuExamQueryDTO stuExamQueryDTO) {
        log.info("学生考试分页条件查询: {}", stuExamQueryDTO);
        PageResult<StuExamVO> pageResult  = examManagementService.pageStuExam(stuExamQueryDTO);
        return ResponseResult.success(pageResult);
    }

}
