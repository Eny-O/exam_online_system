package com.ai.exam_spring.modules.user.controller;

import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.user.pojo.dto.UserAddDTO;
import com.ai.exam_spring.modules.user.pojo.dto.UserQueryDTO;
import com.ai.exam_spring.modules.user.pojo.dto.UserUpdateDTO;
import com.ai.exam_spring.modules.user.pojo.vo.UserManageVO;
import com.ai.exam_spring.modules.user.service.user.IUserManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "人员管理接口")
@RestController
@RequestMapping("/user/manage")
@Slf4j
public class UserManageController {

    @Autowired
    private IUserManageService userManageService;

    @Operation(summary = "分页查询人员列表")
    @PreAuthorize("hasAuthority('sys:user:view')")
    @GetMapping("/page")
    public ResponseResult<PageResult<UserManageVO>> pageUser(UserQueryDTO userQueryDTO) {
        return ResponseResult.success(userManageService.pageUser(userQueryDTO));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @PostMapping("/add")
    public ResponseResult<Void> addUser(@Valid @RequestBody UserAddDTO userAddDTO) {
        userManageService.addUser(userAddDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "修改用户")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    @PutMapping("/update")
    public ResponseResult<Void> updateUser(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        userManageService.updateUser(userUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "切换用户状态")
    @PreAuthorize("hasAuthority('sys:user:status')")
    @PutMapping("/toggleStatus")
    public ResponseResult<Void> toggleStatus(@RequestParam Integer id) {
        userManageService.toggleStatus(id);
        return ResponseResult.success();
    }

    @Operation(summary = "重置密码")
    @PreAuthorize("hasAuthority('sys:user:reset')")
    @PutMapping("/resetPassword")
    public ResponseResult<Void> resetPassword(@RequestParam Integer id) {
        userManageService.resetPassword(id);
        return ResponseResult.success("密码已重置为 123456");
    }
}
