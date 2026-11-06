package com.ai.exam_auth_user.controller;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_auth_user.pojo.dto.StudentQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserLoginDTO;
import com.ai.exam_auth_user.pojo.dto.RegisterDTO;
import com.ai.exam_auth_user.pojo.vo.UserInfoVO;
import com.ai.exam_api.pojo.vo.UStudentVO;
import com.ai.exam_auth_user.service.user.UserService;
import com.ai.exam_auth_user.service.user.IUserManageService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private IUserManageService userManageService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/getInfo")
    public ResponseResult<UserInfoVO> getInfo() {
        log.info("获取用户信息");
        return ResponseResult.success(userService.getInfo());
    }

    @Operation(summary = "用户登录")
    @SentinelResource(value = "login", blockHandler = "loginBlocked")
    @PostMapping("/login")
    public ResponseResult<HashMap<String,String>> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录：{}", userLoginDTO);
        return ResponseResult.success(userService.login(userLoginDTO));
    }

    /**
     * 登录接口限流降级。
     * 签名要求：参数列表与原方法完全一致，末尾追加 BlockException，返回类型相同。
     * 降级响应仍走统一响应封装，前端无需特殊处理。
     */
    public ResponseResult<HashMap<String,String>> loginBlocked(UserLoginDTO userLoginDTO, BlockException ex) {
        log.warn("登录接口触发 Sentinel 限流：{}", ex.getClass().getSimpleName());
        return ResponseResult.error("系统繁忙，请稍后重试");
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ResponseResult<Void> logout() {
        log.info("用户登出");
        userService.logout();
        return ResponseResult.success("登出成功");
    }

    @Operation(summary = "学生自助注册")
    @PostMapping("/register")
    public ResponseResult<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("学生自助注册：{}", registerDTO.getUsername());
        userManageService.register(registerDTO);
        return ResponseResult.success("注册成功");
    }

    @Operation(summary = "批量导入学生")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @PostMapping(value = "/import")
    public ResponseResult<BatchInsertResult> importStudents(
            @RequestPart @RequestParam("file") MultipartFile file) {
        log.info("批量导入学生");
        BatchInsertResult result = userManageService.importStudents(file);
        return ResponseResult.success(result);
    }

    @Operation(summary = "下载学生导入模板")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/user_import_template.xlsx");
        if (!resource.exists()) {
            throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
        }
        String filename = "user_import_template.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @Operation(summary = "根据班级id获取班级成员")
    @PreAuthorize("hasAnyAuthority('sys:user:view', 'tch:class:view')")
    @GetMapping("/getClassMember")
    public ResponseResult<List<UStudentVO>> getClassMember(Integer classId) {
        log.info("根据班级id获取班级成员 {}", classId);
        List<UStudentVO> students = userService.getClassMember(classId);
        return ResponseResult.success(students);
    }

    @Operation(summary = "分页条件查询学生")
    @PreAuthorize("hasAnyAuthority('sys:user:view', 'tch:class:view')")
    @GetMapping("/pageStudent")
    public ResponseResult<PageResult<UStudentVO>> pageStudent(StudentQueryDTO studentQueryDTO) {
        log.info("分页条件查询学生: {}", studentQueryDTO);
        PageResult<UStudentVO> pageResult = userService.pageStudent(studentQueryDTO);
        return ResponseResult.success(pageResult);
    }

}
