package com.ai.exam_spring.modules.user.controller;

import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.user.pojo.dto.StudentQueryDTO;
import com.ai.exam_spring.modules.user.pojo.dto.UserLoginDTO;
import com.ai.exam_spring.modules.user.pojo.vo.UserInfoVO;
import com.ai.exam_spring.modules.user.pojo.vo.UStudentVO;
import com.ai.exam_spring.modules.user.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('sys:user:view')")
    @GetMapping("/getClassMember")
    public ResponseResult<List<UStudentVO>> getClassMember(Integer classId) {
        log.info("根据班级id获取班级成员 {}", classId);
        List<UStudentVO> students = userService.getClassMember(classId);
        return ResponseResult.success(students);
    }

    @Operation(summary = "分页条件查询学生")
    @PreAuthorize("hasAuthority('sys:user:view')")
    @GetMapping("/pageStudent")
    public ResponseResult<PageResult<UStudentVO>> pageStudent(StudentQueryDTO studentQueryDTO) {
        log.info("分页条件查询学生: {}", studentQueryDTO);
        PageResult<UStudentVO> pageResult = userService.pageStudent(studentQueryDTO);
        return ResponseResult.success(pageResult);
    }

}
