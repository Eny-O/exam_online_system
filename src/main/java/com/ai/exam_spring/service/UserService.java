package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.dto.UserLoginDTO;
import com.ai.exam_spring.domain.vo.UserInfoVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public interface UserService {
    /**
     * 登录
     * @param userLoginDTO
     */
    HashMap<String,String> login(UserLoginDTO userLoginDTO);

    void logout();

    UserInfoVO getInfo();
}
