package com.ai.exam_spring.service;

import com.ai.exam_spring.pojo.dto.UserLoginDTO;
import com.ai.exam_spring.pojo.vo.UserInfoVO;
import com.ai.exam_spring.pojo.vo.UStudentVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * 用户服务接口
 */
@Service
public interface UserService {
    /**
     * 用户登录，返回包含 Token 的 Map
     */
    HashMap<String,String> login(UserLoginDTO userLoginDTO);

    /**
     * 用户登出，清除 Redis 登录缓存
     */
    void logout();

    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getInfo();

    List<UStudentVO> getClassMember(Integer classId);
}
