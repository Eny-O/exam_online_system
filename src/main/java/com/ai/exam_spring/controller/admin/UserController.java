package com.ai.exam_spring.controller.admin;

import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.dto.UserLoginDTO;
import com.ai.exam_spring.domain.vo.UserInfoVO;
import com.ai.exam_spring.domain.vo.UStudentVO;
import com.ai.exam_spring.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Operation(summary = "获取用户信息")
    @GetMapping("/getInfo")
    public ResponseResult<UserInfoVO> getInfo() {
        log.info("获取用户信息");
        return ResponseResult.success(userService.getInfo());
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResponseResult<HashMap<String,String>> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录：{}", userLoginDTO);
        return ResponseResult.success(userService.login(userLoginDTO));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ResponseResult<Void> logout() {
        log.info("用户登出");
        userService.logout();
        return ResponseResult.success("登出成功");
    }

    @Operation(summary = "根据班级id获取班级成员")
    @GetMapping("/getClassMember")
    public ResponseResult<List<UStudentVO>> getClassMember(Integer classId) {
        log.info("根据班级id获取班级成员 {}", classId);
        List<UStudentVO> students = userService.getClassMember(classId);
        return ResponseResult.success(students);
    }
}
