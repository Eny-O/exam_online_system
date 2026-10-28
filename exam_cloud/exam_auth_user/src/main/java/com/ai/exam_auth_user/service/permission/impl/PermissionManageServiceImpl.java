package com.ai.exam_auth_user.service.permission.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_auth_user.pojo.dto.PermissionAddDTO;
import com.ai.exam_auth_user.pojo.dto.PermissionUpdateDTO;
import com.ai.exam_auth_user.pojo.dos.Permission;
import com.ai.exam_auth_user.pojo.vo.PermissionTreeVO;
import com.ai.exam_auth_user.mapper.PermissionMapper;
import com.ai.exam_auth_user.service.permission.IPermissionManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionManageServiceImpl extends ServiceImpl<PermissionMapper, Permission>
        implements IPermissionManageService {

    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        // 查询全部权限，按 id 排序
        List<Permission> all = list(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));

        // 转换为 VO
        List<PermissionTreeVO> voList = all.stream().map(p -> {
            PermissionTreeVO vo = new PermissionTreeVO();
            BeanUtil.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());

        // 构建树形结构
        Map<Integer, List<PermissionTreeVO>> parentMap = voList.stream()
                .collect(Collectors.groupingBy(PermissionTreeVO::getParentId));

        // 给每个节点设置 children
        voList.forEach(vo -> vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>())));

        // 返回顶级节点（parentId = 0）
        return voList.stream()
                .filter(vo -> vo.getParentId() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public void addPermission(PermissionAddDTO dto) {
        // 检查 permCode 唯一性
        long count = count(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermCode, dto.getPermCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.OPERATION_FAILED, "权限编码已存在");
        }

        Permission permission = new Permission();
        BeanUtil.copyProperties(dto, permission);
        save(permission);
    }

    @Override
    public void updatePermission(PermissionUpdateDTO dto) {
        Permission existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "权限不存在");
        }

        // 检查 permCode 唯一性（排除自身）
        long count = count(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermCode, dto.getPermCode())
                .ne(Permission::getId, dto.getId()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.OPERATION_FAILED, "权限编码已存在");
        }

        BeanUtil.copyProperties(dto, existing, "id");
        updateById(existing);
    }

    @Override
    public void deletePermission(Integer id) {
        Permission existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "权限不存在");
        }

        // 检查是否存在子级权限
        long childCount = count(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCodeEnum.OPERATION_FAILED, "该权限存在子级，无法删除");
        }

        removeById(id);
    }
}
