package com.ai.exam_api.feign;

import com.ai.exam_common.common.pojo.result.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * exam-core 考试班级关联查询（只读）
 */
@FeignClient(name = "exam-core", contextId = "examClassFeignClient")
public interface ExamClassFeignClient {

    /**
     * 查询班级是否被考试引用
     *
     * @param classId 班级ID
     * @return 是否存在引用
     */
    @GetMapping("/inner/class/exam-refs")
    ResponseResult<Boolean> hasClassReference(@RequestParam("classId") Integer classId);
}
