package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.dto.ExamDetailDTO;
import com.ai.exam_spring.domain.dto.ExamQueryDTO;
import com.ai.exam_spring.domain.enums.ExamEnum;
import com.ai.exam_spring.domain.vo.ExamVO;
import com.ai.exam_spring.service.ExamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.stream;

@Tag(name = "考试管理接口")
@RestController
@RequestMapping("/exam-task-management")
@Slf4j
public class ExamManagementController {

    @Autowired
    private ExamManagementService  examManagementService;

    @Operation(summary = "考试分页条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<ExamVO>> page(ExamQueryDTO examQueryDTO) {
        log.info("考试分页条件查询 {}",  examQueryDTO);
        PageResult<ExamVO> pageResult = examManagementService.pageExam(examQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "增加试卷")
    @PostMapping("/add")
    public ResponseResult<Integer> add(@RequestBody ExamDetailDTO examDetailDTO) {
        log.info("增加试卷 {}", examDetailDTO);
        Integer id = examManagementService.addExam(examDetailDTO);
        return ResponseResult.success(id);
    }


    @Operation(summary = "获取所有考试状态",description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-已结束,5-待批阅,6-批阅完成,其中2,3,4前端维护")
    @GetMapping("/getAllExamStatus")
    public ResponseResult<ExamEnum[]> getAllExamStatus() {
        log.info("获取所有考试状态");
        ExamEnum[] codes = ExamEnum.values();
        return ResponseResult.success(codes);
    }



}
