package com.ai.exam_auth_user.controller;

import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_auth_user.pojo.dos.Role;
import com.ai.exam_auth_user.pojo.vo.PermissionTreeVO;
import com.ai.exam_auth_user.service.roleAuth.IRoleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色权限分配接口")
@RestController
@RequestMapping("/role/auth")
@Slf4j
public class RoleAuthController {

    @Autowired
    private IRoleAuthService roleAuthService;

    @Operation(summary = "查询所有角色")
    @PreAuthorize("hasAuthority('sys:perm:view')")
    @GetMapping("/list")
    public ResponseResult<List<Role>> listRoles() {
        log.info("查询所有角色");
        return ResponseResult.success(roleAuthService.listRoles());
    }

    @Operation(summary = "查询完整权限树")
    @PreAuthorize("hasAuthority('sys:perm:view')")
    @GetMapping("/permissions")
    public ResponseResult<List<PermissionTreeVO>> getPermissionTree() {
        log.info("查询完整权限树");
        return ResponseResult.success(roleAuthService.getPermissionTree());
    }

    @Operation(summary = "查询角色已分配的权限ID")
    @PreAuthorize("hasAuthority('sys:perm:view')")
    @GetMapping("/permissions/{roleId}")
    public ResponseResult<List<Integer>> getRolePermissionIds(@PathVariable Integer roleId) {
        log.info("查询角色已分配的权限ID: {}", roleId);
        return ResponseResult.success(roleAuthService.getRolePermissionIds(roleId));
    }

    @Operation(summary = "为角色分配权限")
    @PreAuthorize("hasAuthority('sys:perm:edit')")
    @PutMapping("/assign")
    public ResponseResult<Void> assignPermissions(@RequestBody AssignDTO dto) {
        log.info("为角色分配权限: {}", dto.getRoleId());
        roleAuthService.assignPermissions(dto.getRoleId(), dto.getPermissionIds());
        return ResponseResult.success();
    }

    @Data
    public static class AssignDTO {
        private Integer roleId;
        private List<Integer> permissionIds;
    }
}
