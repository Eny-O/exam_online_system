package com.ai.exam_spring.controller.admin;

import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.dto.permission.PermissionAddDTO;
import com.ai.exam_spring.pojo.dto.permission.PermissionUpdateDTO;
import com.ai.exam_spring.pojo.vo.permission.PermissionTreeVO;
import com.ai.exam_spring.service.permission.IPermissionManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "权限管理接口")
@RestController
@RequestMapping("/permission/manage")
@Slf4j
public class PermissionManageController {

    @Autowired
    private IPermissionManageService permissionManageService;

    @Operation(summary = "查询权限树形列表")
    @PreAuthorize("hasAuthority('sys:perm:view')")
    @GetMapping("/tree")
    public ResponseResult<List<PermissionTreeVO>> getPermissionTree() {
        return ResponseResult.success(permissionManageService.getPermissionTree());
    }

    @Operation(summary = "新增权限")
    @PreAuthorize("hasAuthority('sys:perm:add')")
    @PostMapping("/add")
    public ResponseResult<Void> addPermission(@Valid @RequestBody PermissionAddDTO dto) {
        permissionManageService.addPermission(dto);
        return ResponseResult.success();
    }

    @Operation(summary = "修改权限")
    @PreAuthorize("hasAuthority('sys:perm:edit')")
    @PutMapping("/update")
    public ResponseResult<Void> updatePermission(@Valid @RequestBody PermissionUpdateDTO dto) {
        permissionManageService.updatePermission(dto);
        return ResponseResult.success();
    }

    @Operation(summary = "删除权限")
    @PreAuthorize("hasAuthority('sys:perm:delete')")
    @DeleteMapping("/delete")
    public ResponseResult<Void> deletePermission(@RequestParam Integer id) {
        permissionManageService.deletePermission(id);
        return ResponseResult.success();
    }
}
