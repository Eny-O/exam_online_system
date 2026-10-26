package com.ai.exam_spring.modules.user.mapper;

import com.ai.exam_spring.modules.user.pojo.dos.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询指定角色已关联的权限ID列表
     */
    List<Integer> getPermissionIdsByRoleId(@Param("roleId") Integer roleId);

    /**
     * 删除角色的全部权限关联
     */
    void deleteRolePermissions(@Param("roleId") Integer roleId);

    /**
     * 批量插入角色-权限关联
     */
    void batchInsertRolePermissions(@Param("roleId") Integer roleId, @Param("permissionIds") List<Integer> permissionIds);
}
