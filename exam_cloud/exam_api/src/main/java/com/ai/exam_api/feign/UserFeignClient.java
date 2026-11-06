package com.ai.exam_api.feign;

import com.ai.exam_api.pojo.dto.UserBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * auth-user 用户查询（只读）
 */
@FeignClient(name = "exam-auth-user", contextId = "userFeignClient")
public interface UserFeignClient {

    /**
     * 按用户ID批量查询用户摘要
     *
     * @param userIds 用户ID列表；为空时服务端直接返回空列表
     * @return 用户摘要列表
     */
    @GetMapping("/inner/user/briefs")
    List<UserBriefDTO> listUserBriefs(@RequestParam("userIds") List<Integer> userIds);

    /**
     * 查询教师任教班级ID
     *
     * @param userId 教师用户ID
     * @return 任教班级ID列表
     */
    @GetMapping("/inner/user/teacher-class-ids")
    List<Integer> listTeacherClassIds(@RequestParam("userId") Integer userId);
}
