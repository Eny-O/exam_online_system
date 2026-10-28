// UserFeignClientFallbackFactory.java
package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.UserBriefDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;   // 加不加都行，Sentinel 会 new 实例
import java.util.Collections;
import java.util.List;

@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {
    private static final Logger log = LoggerFactory.getLogger(UserFeignClientFallbackFactory.class);

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public List<UserBriefDTO> listUserBriefs(List<Integer> userIds) {
                log.warn("UserFeignClient 降级: 调用 auth-user /inner/user/briefs 失败, 返回空列表. cause={}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
