package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.StudentAnswerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 学生作答查询 Feign 客户端（exam-core）
 */
@FeignClient(name = "exam-core", contextId = "studentAnswerFeignClient", fallbackFactory = StudentAnswerFeignClientFallbackFactory.class)
public interface StudentAnswerFeignClient {

    @GetMapping("/inner/student-answer")
    List<StudentAnswerDTO> listByExamAndStudent(@RequestParam("examId") Integer examId,
                                                @RequestParam("studentId") Integer studentId);
}
