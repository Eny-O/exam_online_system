package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.QuestionBriefDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * QuestionFeignClient 降级工厂
 */
@Component
public class QuestionFeignClientFallbackFactory implements FallbackFactory<QuestionFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(QuestionFeignClientFallbackFactory.class);

    @Override
    public QuestionFeignClient create(Throwable cause) {
        return new QuestionFeignClient() {
            @Override
            public List<QuestionBriefDTO> listByIds(List<Integer> ids) {
                log.warn("QuestionFeignClient 降级: 调用 exam-core /inner/question/batch 失败, 返回空列表. cause={}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
