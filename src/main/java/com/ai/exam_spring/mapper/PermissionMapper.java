package com.ai.exam_spring.mapper;

import com.ai.exam_spring.pojo.entity.user.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 权限表 Mapper 接口
 */
public interface PermissionMapper extends BaseMapper<Permission> {
    /**
     * 根据用户ID查询权限列表（含菜单和按钮）
     */
    List<Permission> getPermissionsByUserId(Integer userId);
}
