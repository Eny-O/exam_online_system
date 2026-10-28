package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.ClassBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * auth-user 班级查询（只读）
 */
@FeignClient(name = "exam-auth-user", contextId = "classFeignClient", fallbackFactory = ClassFeignClientFallbackFactory.class)
public interface ClassFeignClient {

    /**
     * 按班级ID批量查询班级摘要
     *
     * @param classIds 班级ID列表；为空时服务端直接返回空列表
     * @return 班级摘要列表
     */
    @GetMapping("/inner/class/briefs")
    List<ClassBriefDTO> listClassBriefs(@RequestParam("classIds") List<Integer> classIds);
}
