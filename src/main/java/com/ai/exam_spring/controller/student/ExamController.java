package com.ai.exam_spring.controller.student;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.exam.StuExamQueryDTO;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.pojo.vo.exam.ExamResultVO;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.StuExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "学生考试相关接口")
@RestController
@RequestMapping("/stu/exam")
@Slf4j
public class ExamController {

    @Autowired
    private IExamManagementService examManagementService;

    @Autowired
    private StuExamService stuExamService;

    @Operation(summary = "学生考试分页条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<StuExamVO>> page(StuExamQueryDTO stuExamQueryDTO) {
        log.info("学生考试分页条件查询: {}", stuExamQueryDTO);
        PageResult<StuExamVO> pageResult  = examManagementService.pageStuExam(stuExamQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "学生查看考试详情")
    @GetMapping("/getExamInfo")
    public ResponseResult<StuExamVO> getExamInfoById(@RequestParam Integer id) {
        log.info("学生查看考试详情: {}", id);
        StuExamVO stuExamVO = stuExamService.examInfoById(id);
        return ResponseResult.success(stuExamVO);
    }

    @Operation(summary = "学生获取试卷内容")
    @GetMapping("/getExamPaper")
    public ResponseResult<StuPaperVO> getPaperById(@RequestParam Integer id) {
        log.info("学生获取试卷内容: {}", id);
        StuPaperVO stuPaperVO = stuExamService.getPaperById(id);
        return ResponseResult.success(stuPaperVO);
    }

    @Operation(summary = "学生提交试卷")
    @PostMapping("/submitPaper")
    public ResponseResult<Void> submitPaper(@RequestBody List<StudentUpdateAnswerDTO> studentUpdateAnswerDTOS) {
        log.info("学生提交试卷: {}", studentUpdateAnswerDTOS);
        stuExamService.submitPaper(studentUpdateAnswerDTOS);
        return ResponseResult.success();
    }

    @Operation(summary = "学生获取答题详情")
    @GetMapping("/getAnswerDetail")
    public ResponseResult<List<BaseStudentAnswerVO>> getAnswerDetail(@RequestParam Integer examId) {
        log.info("学生获取答题详情: {}", examId);
        List<BaseStudentAnswerVO> baseStudentAnswerVOS = stuExamService.getAnswerDetail(examId);
        return ResponseResult.success(baseStudentAnswerVOS);
    }

    @Operation(summary = "学生获取考试结果")
    @GetMapping("/getExamResult")
    public ResponseResult<ExamResultVO> getExamResult(@RequestParam Integer examId) {
        log.info("学生获取考试结果: {}", examId);
        ExamResultVO examResultVO = stuExamService.getExamResult(examId);
        return ResponseResult.success(examResultVO);
    }

    @Operation(summary = "学生获取所有考试成绩")
    @GetMapping("/getAllExamResult")
    public ResponseResult<List<ExamResultVO>> getAllExamResult() {
        log.info("学生获取所有考试成绩");
        List<ExamResultVO> examResultVOs = stuExamService.getAllExamResult();
        return ResponseResult.success(examResultVOs);
    }

    @Operation(summary = "初始化学生考试数据",description = "返回的数据为(questionId->id),id为学生答案的主键id,学生修改答案会用到")
    @GetMapping("/initStuExam")
    public ResponseResult<Map<Integer, Integer>> initStuExam(@RequestParam Integer examId) {
        log.info("初始化学生考试数据: {}", examId);
        return ResponseResult.success(stuExamService.initStuExam(examId));
    }

    @Operation(summary = "学生修改答案")
    @PutMapping("/updateAnswer")
    public ResponseResult<Void> updateAnswer(@RequestBody StudentUpdateAnswerDTO studentUpdateAnswerDTO) {
        log.info("学生修改答案: {}", studentUpdateAnswerDTO);
        stuExamService.updateBatchAnswer(List.of(studentUpdateAnswerDTO));
        return ResponseResult.success();
    }


}
