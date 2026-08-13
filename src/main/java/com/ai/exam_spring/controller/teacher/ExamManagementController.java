package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.exam.*;
import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import com.ai.exam_spring.pojo.vo.exam.ExamDetailVO;
import com.ai.exam_spring.pojo.vo.exam.ExamVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "考试管理接口")
@RestController
@RequestMapping("/exam-task-management")
@Slf4j
public class ExamManagementController {

    @Autowired
    private IExamManagementService examManagementService;

    @Operation(summary = "考试分页条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<ExamVO>> page(@Validated ExamQueryDTO examQueryDTO) {
        log.info("考试分页条件查询 {}",  examQueryDTO);
        PageResult<ExamVO> pageResult = examManagementService.pageExam(examQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "增加考试")
    @PostMapping("/add")
    public ResponseResult<Integer> add(@Validated @RequestBody ExamDetailAddDTO examDetailAddDTO) {
        log.info("增加试卷 {}", examDetailAddDTO);
        Integer id = examManagementService.addExam(examDetailAddDTO);
        return ResponseResult.success(id);
    }

    @Operation(summary = "更新考试")
    @PutMapping("/update")
    public ResponseResult<Void> update(@Validated @RequestBody ExamDetailUpdateDTO examDetailUpdateDTO) {
        log.info("更新试卷 {}", examDetailUpdateDTO);
        examManagementService.updateExam(examDetailUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "根据id获取考试详情")
    @GetMapping("/getExamDetailById")
    public ResponseResult<ExamDetailVO> getExamDetailById(@Validated @RequestParam Integer id) {
        log.info("根据id获取考试详情 {}", id);
        ExamDetailVO examDetailVO = examManagementService.getExamDetailById(id);
        return ResponseResult.success(examDetailVO);
    }


    @Operation(summary = "获取所有考试状态",description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成,其中2,3,4前端维护")
    @GetMapping("/getAllExamStatus")
    public ResponseResult<ExamEnum[]> getAllExamStatus() {
        log.info("获取所有考试状态");
        ExamEnum[] codes = ExamEnum.values();
        return ResponseResult.success(codes);
    }

    @Operation(summary = "更改考试的状态", description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-已结束,5-待批阅,6-批阅完成,其中2,3,4前端维护")
    @PutMapping("/updateExamStatus")
    public ResponseResult<Void> updateExamStatus(@Validated ExamStatusUpdateDTO examStatusUpdateDTO) {
        log.info("更新考试状态: {}", examStatusUpdateDTO);
        examManagementService.updateExamStatus(examStatusUpdateDTO);
        return ResponseResult.success();
    }



}
