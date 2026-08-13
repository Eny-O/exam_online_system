package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerAddDTO;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerUpdateDTO;
import com.ai.exam_spring.pojo.vo.grade.GradePaperContentVO;
import com.ai.exam_spring.pojo.vo.grade.GradeStudentVO;
import com.ai.exam_spring.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.service.grading.GradePaperService;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gradePaper")
@Tag(name = "教师端阅卷管理")
@Slf4j
public class GradePaperController {

    @Autowired
    private GradePaperService gradePaperService;

    @Autowired
    private IStudentAnswerService studentAnswerService;

    @Operation(summary = "获取批阅试卷")
    @GetMapping("/getGradePaper")
    public ResponseResult<GradePaperContentVO> getGradePaper(@Validated @RequestParam Integer examId) {
        log.info("获取批阅试卷 {}", examId);
        GradePaperContentVO gradePaperContentVO = gradePaperService.getGradePaper(examId);
        return ResponseResult.success(gradePaperContentVO);
    }

    @Operation(summary = "获取学生答题情况")
    @GetMapping("/getStudentAnswer")
    public ResponseResult<List<StudentAnswerVO>> getStudentAnswer(@Validated @RequestParam Integer studentId, Integer examId) {
        log.info("获取学生答题情况 {}, {}", studentId, examId);
        List<StudentAnswerVO> studentAnswerVOS = studentAnswerService.getStudentAnswer(studentId, examId);
        return ResponseResult.success(studentAnswerVOS);
    }

    @Operation(summary = "获取要批阅学生列表")
    @GetMapping("/getGradeStudentList")
    public ResponseResult<List<GradeStudentVO>> getGradeStudentList(@Validated @RequestParam Integer examId) {
        log.info("获取要批阅学生列表 {}", examId);
        List<GradeStudentVO> gradeStudentVOS = gradePaperService.getGradeStudentList(examId);
        return ResponseResult.success(gradeStudentVOS);
    }

    @Operation(summary = "更新批阅的学生得分")
    @PutMapping("/updateGradeStudentScore")
    public ResponseResult<Void> updateGradeStudentScore(@RequestBody List<StudentAnswerUpdateDTO> studentAnswerUpdateDTOS) {
        log.info("更新批阅的学生得分 {}", studentAnswerUpdateDTOS);
        studentAnswerService.updateScoreAndGrade(studentAnswerUpdateDTOS);
        return ResponseResult.success();
    }

}
