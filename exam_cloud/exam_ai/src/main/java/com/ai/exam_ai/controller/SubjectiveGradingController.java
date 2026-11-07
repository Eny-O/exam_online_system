package com.ai.exam_ai.controller;

import com.ai.exam_ai.pojo.dto.SubjectiveGradeRequest;
import com.ai.exam_ai.service.SubjectiveGradeOutcome;
import com.ai.exam_ai.service.SubjectiveGradingService;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主观题智能批阅接口（对外可验证入口，窄接口方案：单 Controller 多方法聚合）
 */
@RestController
@RequestMapping("/ai/subjective")
@RequiredArgsConstructor
@Slf4j
public class SubjectiveGradingController {

    private final SubjectiveGradingService subjectiveGradingService;

    @PostMapping("/grade")
    public ResponseResult<SubjectiveGradeOutcome> grade(@Valid @RequestBody SubjectiveGradeRequest request) {
        log.info("主观题智能批阅: examId={}, studentId={}, questionId={}",
                request.getExamId(), request.getStudentId(), request.getQuestionId());
        return ResponseResult.success(subjectiveGradingService.gradeByIds(
                request.getExamId(), request.getStudentId(), request.getQuestionId()));
    }
}
