package com.ai.exam_spring.mapper;

import com.ai.exam_spring.domain.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {
    List<Permission> getPermissionsByUserId(Long userId);
}
