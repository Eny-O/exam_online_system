package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.StudentAnswerDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * StudentAnswerFeignClient 降级工厂
 */
@Component
public class StudentAnswerFeignClientFallbackFactory implements FallbackFactory<StudentAnswerFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(StudentAnswerFeignClientFallbackFactory.class);

    @Override
    public StudentAnswerFeignClient create(Throwable cause) {
        return new StudentAnswerFeignClient() {
            @Override
            public List<StudentAnswerDTO> listByExamAndStudent(Integer examId, Integer studentId) {
                log.warn("StudentAnswerFeignClient 降级: 调用 exam-core /inner/student-answer 失败, 返回空列表. cause={}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
