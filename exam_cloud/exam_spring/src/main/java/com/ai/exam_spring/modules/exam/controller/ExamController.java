package com.ai.exam_spring.modules.exam.controller;

import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.exam.pojo.dto.StuExamQueryDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.StudentUpdateAnswerDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamResultVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.BaseStudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.exam.service.StuExamService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/page")
    public ResponseResult<PageResult<StuExamVO>> page(StuExamQueryDTO stuExamQueryDTO) {
        log.info("学生考试分页条件查询: {}", stuExamQueryDTO);
        PageResult<StuExamVO> pageResult  = examManagementService.pageStuExam(stuExamQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "学生查看考试详情")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/getExamInfo")
    public ResponseResult<StuExamVO> getExamInfoById(@RequestParam Integer id) {
        log.info("学生查看考试详情: {}", id);
        StuExamVO stuExamVO = stuExamService.examInfoById(id);
        return ResponseResult.success(stuExamVO);
    }

    @Operation(summary = "学生获取试卷内容")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/getExamPaper")
    public ResponseResult<StuPaperVO> getPaperById(@RequestParam Integer id) {
        log.info("学生获取试卷内容: {}", id);
        StuPaperVO stuPaperVO = stuExamService.getPaperById(id);
        return ResponseResult.success(stuPaperVO);
    }

    @Operation(summary = "学生提交试卷（从Redis读取答案批量写入DB，更新状态，自动批阅客观题）")
    @PreAuthorize("hasAuthority('stu:exam:submit')")
    @SentinelResource(value = "submitPaper", blockHandler = "submitPaperBlocked")
    @PostMapping("/submitPaper")
    public ResponseResult<Void> submitPaper(@RequestParam Integer examId) {
        log.info("学生提交试卷, examId: {}", examId);
        stuExamService.submitPaper(examId);
        return ResponseResult.success();
    }

    /**
     * 交卷接口限流降级。
     * 签名要求：参数列表与原方法完全一致，末尾追加 BlockException，返回类型相同。
     * 降级响应仍走统一响应封装，前端无需特殊处理。
     */
    public ResponseResult<Void> submitPaperBlocked(Integer examId, BlockException ex) {
        log.warn("交卷接口触发 Sentinel 限流, examId: {}, cause: {}", examId, ex.getClass().getSimpleName());
        return ResponseResult.error("交卷请求过于频繁，请稍后重试");
    }

    @Operation(summary = "学生获取答题详情")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/getAnswerDetail")
    public ResponseResult<List<BaseStudentAnswerVO>> getAnswerDetail(@RequestParam Integer examId) {
        log.info("学生获取答题详情: {}", examId);
        List<BaseStudentAnswerVO> baseStudentAnswerVOS = stuExamService.getAnswerDetail(examId);
        return ResponseResult.success(baseStudentAnswerVOS);
    }

    @Operation(summary = "学生获取考试结果")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/getExamResult")
    public ResponseResult<ExamResultVO> getExamResult(@RequestParam Integer examId) {
        log.info("学生获取考试结果: {}", examId);
        ExamResultVO examResultVO = stuExamService.getExamResult(examId);
        return ResponseResult.success(examResultVO);
    }

    @Operation(summary = "学生获取所有考试成绩")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/getAllExamResult")
    public ResponseResult<List<ExamResultVO>> getAllExamResult() {
        log.info("学生获取所有考试成绩");
        List<ExamResultVO> examResultVOs = stuExamService.getAllExamResult();
        return ResponseResult.success(examResultVOs);
    }

    @Operation(summary = "初始化学生考试", description = "校验考试和学生信息，答案由答题时写入Redis暂存")
    @PreAuthorize("hasAuthority('stu:exam:view')")
    @GetMapping("/initStuExam")
    public ResponseResult<Void> initStuExam(@RequestParam Integer examId) {
        log.info("初始化学生考试: {}", examId);
        stuExamService.initStuExam(examId);
        return ResponseResult.success();
    }

    @Operation(summary = "学生修改答案（写入Redis暂存）")
    @PreAuthorize("hasAuthority('stu:exam:answer')")
    @PutMapping("/updateAnswer")
    public ResponseResult<Void> updateAnswer(@RequestBody StudentUpdateAnswerDTO studentUpdateAnswerDTO) {
        log.info("学生修改答案: {}", studentUpdateAnswerDTO);
        stuExamService.updateBatchAnswer(List.of(studentUpdateAnswerDTO));
        return ResponseResult.success();
    }


}
