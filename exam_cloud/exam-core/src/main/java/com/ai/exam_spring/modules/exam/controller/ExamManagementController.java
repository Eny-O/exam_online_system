package com.ai.exam_spring.modules.exam.controller;

import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamDetailAddDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamDetailUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamQueryDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamStatusUpdateDTO;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamDetailVO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('tch:exam:view')")
    @GetMapping("/page")
    public ResponseResult<PageResult<ExamVO>> page(@Validated ExamQueryDTO examQueryDTO) {
        log.info("考试分页条件查询 {}",  examQueryDTO);
        PageResult<ExamVO> pageResult = examManagementService.pageExam(examQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "增加考试")
    @PreAuthorize("hasAuthority('tch:exam:add')")
    @PostMapping("/add")
    public ResponseResult<Integer> add(@Validated @RequestBody ExamDetailAddDTO examDetailAddDTO) {
        log.info("增加试卷 {}", examDetailAddDTO);
        Integer id = examManagementService.addExam(examDetailAddDTO);
        return ResponseResult.success(id);
    }

    @Operation(summary = "更新考试")
    @PreAuthorize("hasAuthority('tch:exam:edit')")
    @PutMapping("/update")
    public ResponseResult<Void> update(@Validated @RequestBody ExamDetailUpdateDTO examDetailUpdateDTO) {
        log.info("更新试卷 {}", examDetailUpdateDTO);
        examManagementService.updateExam(examDetailUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "根据id获取考试详情")
    @PreAuthorize("hasAuthority('tch:exam:view')")
    @GetMapping("/getExamDetailById")
    public ResponseResult<ExamDetailVO> getExamDetailById(@Validated @RequestParam Integer id) {
        log.info("根据id获取考试详情 {}", id);
        ExamDetailVO examDetailVO = examManagementService.getExamDetailById(id);
        return ResponseResult.success(examDetailVO);
    }


    @Operation(summary = "获取所有考试状态",description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成,其中2,3,4前端维护")
    @PreAuthorize("hasAuthority('tch:exam:view')")
    @GetMapping("/getAllExamStatus")
    public ResponseResult<ExamEnum[]> getAllExamStatus() {
        log.info("获取所有考试状态");
        ExamEnum[] codes = ExamEnum.values();
        return ResponseResult.success(codes);
    }

    @Operation(summary = "发布考试")
    @PreAuthorize("hasAuthority('tch:exam:status')")
    @PutMapping("/publishExam")
    public ResponseResult<Void> publishExam(@Validated @RequestParam Integer id) {
        log.info("发布考试 {}", id);
        examManagementService.publishExam(id);
        return ResponseResult.success();
    }

    @Operation(summary = "取消发布考试")
    @PreAuthorize("hasAuthority('tch:exam:status')")
    @DeleteMapping("/clearExamCache")
    public ResponseResult<Void> clearExamCache(@Validated @RequestParam Integer id) {
        log.info("取消发布考试 {}", id);
        examManagementService.clearExamCache(id);
        return ResponseResult.success();
    }

    @Operation(summary = "更改考试的状态", description = "状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成,其中2,3,4前端维护")
    @PreAuthorize("hasAuthority('tch:exam:status')")
    @PutMapping("/updateExamStatus")
    public ResponseResult<Void> updateExamStatus(@Validated ExamStatusUpdateDTO examStatusUpdateDTO) {
        log.info("更新考试状态: {}", examStatusUpdateDTO);
        examManagementService.updateExamStatus(examStatusUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "删除考试")
    @PreAuthorize("hasAuthority('tch:exam:delete')")
    @DeleteMapping("/deleteExam")
    public ResponseResult<Void> deleteExam(@Validated @RequestParam Integer id) {
        log.info("删除考试 {}", id);
        examManagementService.deleteExam(id);
        return ResponseResult.success();
    }

}
