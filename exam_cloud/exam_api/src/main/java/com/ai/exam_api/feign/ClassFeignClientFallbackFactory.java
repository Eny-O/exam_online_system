package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.ClassBriefDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

public class ClassFeignClientFallbackFactory implements FallbackFactory<ClassFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(ClassFeignClientFallbackFactory.class);
    @Override
    public ClassFeignClient create(Throwable cause) {
        return new ClassFeignClient() {
            @Override
            public List<ClassBriefDTO> listClassBriefs(List<Integer> classIds) {
                log.warn("ClassFeignClient 降级: 调用 auth-user /inner/class/briefs 失败, 返回空列表. cause={}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
