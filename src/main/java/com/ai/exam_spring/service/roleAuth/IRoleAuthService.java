package com.ai.exam_spring.service.roleAuth;

import com.ai.exam_spring.pojo.entity.user.Role;
import com.ai.exam_spring.pojo.vo.permission.PermissionTreeVO;

import java.util.List;

public interface IRoleAuthService {

    /**
     * 查询所有角色
     */
    List<Role> listRoles();

    /**
     * 查询完整权限树
     */
    List<PermissionTreeVO> getPermissionTree();

    /**
     * 查询指定角色已关联的权限ID列表
     */
    List<Integer> getRolePermissionIds(Integer roleId);

    /**
     * 为角色分配权限（全量替换）
     */
    void assignPermissions(Integer roleId, List<Integer> permissionIds);
}
