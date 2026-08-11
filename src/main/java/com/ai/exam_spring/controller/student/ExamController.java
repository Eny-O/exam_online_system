package com.ai.exam_spring.controller.student;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.exam.StuExamQueryDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.pojo.vo.paper.StuPaperVO;
import com.ai.exam_spring.service.exam.IExamManagementService;
import com.ai.exam_spring.service.exam.StuExamService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
