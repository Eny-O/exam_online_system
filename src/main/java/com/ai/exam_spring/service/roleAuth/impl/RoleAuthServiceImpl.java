package com.ai.exam_spring.service.roleAuth.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.pojo.entity.user.Permission;
import com.ai.exam_spring.pojo.entity.user.Role;
import com.ai.exam_spring.pojo.vo.permission.PermissionTreeVO;
import com.ai.exam_spring.mapper.PermissionMapper;
import com.ai.exam_spring.mapper.RoleMapper;
import com.ai.exam_spring.service.roleAuth.IRoleAuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleAuthServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleAuthService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public List<Role> listRoles() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));

        List<PermissionTreeVO> voList = all.stream().map(p -> {
            PermissionTreeVO vo = new PermissionTreeVO();
            BeanUtil.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());

        Map<Integer, List<PermissionTreeVO>> parentMap = voList.stream()
                .collect(Collectors.groupingBy(PermissionTreeVO::getParentId));

        voList.forEach(vo -> vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>())));

        return voList.stream()
                .filter(vo -> vo.getParentId() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getRolePermissionIds(Integer roleId) {
        return roleMapper.getPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Integer roleId, List<Integer> permissionIds) {
        // 先删后插，全量替换
        roleMapper.deleteRolePermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.batchInsertRolePermissions(roleId, permissionIds);
        }
    }
}
