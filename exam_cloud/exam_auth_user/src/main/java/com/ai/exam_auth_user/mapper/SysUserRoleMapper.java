package com.ai.exam_auth_user.mapper;

import com.ai.exam_auth_user.pojo.dos.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联表 Mapper
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 插入用户角色关联
     */
    void insertUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);
}
