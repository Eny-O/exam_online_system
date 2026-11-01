package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.QuestionBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 题目查询 Feign 客户端（exam-core）
 */
@FeignClient(name = "exam-core", contextId = "questionFeignClient")
public interface QuestionFeignClient {

    @GetMapping("/inner/question/batch")
    List<QuestionBriefDTO> listByIds(@RequestParam("ids") List<Integer> ids);
}
